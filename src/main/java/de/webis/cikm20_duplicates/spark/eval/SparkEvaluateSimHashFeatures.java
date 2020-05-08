package de.webis.cikm20_duplicates.spark.eval;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import de.aitools.ir.fingerprinting.representer.Hash;
import de.webis.cikm20_duplicates.spark.SparkCalculateCanonicalLinkGraphEdgeLabels.CanonicalLinkGraphEdge2;
import de.webis.cikm20_duplicates.util.HashTransformationUtil;
import de.webis.cikm20_duplicates.util.FingerPrintUtil;
import de.webis.cikm20_duplicates.util.FingerPrintUtil.Fingerprinter;
import de.webis.trec_ndd.trec_collections.CollectionDocument;
import de.webis.trec_ndd.util.NGramms;
import lombok.Data;
import lombok.SneakyThrows;
import scala.Tuple2;

public class SparkEvaluateSimHashFeatures {

	private static final String DIR = "cikm2020/canonical-link-graph/";
	
	private static final String[] CORPORA = new String[] {"cw09"/*, "cw12", "cc-2015-11"*/};

	public static void main(String[] args) {
		try (JavaSparkContext context = context()) {
			for(String corpus : CORPORA) {
				JavaRDD<String> input = context.textFile(DIR + corpus + "-calulated-edges-sampled-large-groups");
				
				reportFeatureSetEvaluation(input, FingerPrintUtil.simHashFingerPrinting(64, 3), 0.8)
					.saveAsTextFile(DIR + corpus + "-feature-set-evaluation");
			}
		}
	}
	
	private static JavaSparkContext context() {
		SparkConf conf = new SparkConf(true);
		conf.setAppName("cikm2020/evaluate-features");
	
		return new JavaSparkContext(conf);
	}
	
	public static Map<String, List<String>> allFeatures(CollectionDocument doc) {
		Map<String, List<String>> ret = new LinkedHashMap<>();
		
		List<String> oneGramms = nGramms(doc, 1);
		List<String> threeGramms = nGramms(doc, 3);
		List<String> fiveGramms = nGramms(doc, 5);
		List<String> eightGramms = nGramms(doc, 8);
		
		ret.put("1-gramms", oneGramms);
		ret.put("3-gramms", threeGramms);
		ret.put("5-gramms", fiveGramms);
		ret.put("8-gramms", eightGramms);

		ret.put("1-3-gramms", combine(oneGramms, threeGramms));
		ret.put("1-5-gramms", combine(oneGramms, fiveGramms));
		ret.put("1-8-gramms", combine(oneGramms, eightGramms));
		
		ret.put("3-5-gramms", combine(threeGramms, fiveGramms));
		ret.put("3-8-gramms", combine(threeGramms, eightGramms));
		
		ret.put("5-8-gramms", combine(fiveGramms, eightGramms));
		
		return ret;
	}
	
	private static List<String> nGramms(CollectionDocument doc, int length) {
		return Collections.unmodifiableList(new ArrayList<>(NGramms.nGramms(doc.getFullyCanonicalizedContent(), length)));
	}
	
	private static List<String> combine(List<String> a, List<String> b) {
		List<String> ret = new ArrayList<>(a);
		ret.addAll(b);
		
		return ret;
	}
	
	public static JavaRDD<FeatureSetCandidate> featureSetCandidates(JavaRDD<String> input, Fingerprinter<Integer> fingerprinter) {
		JavaPairRDD<String, SimHashDocumentFeatures> hashToDocFeatures = featureHashToDocToFeatures(input, fingerprinter);
		
		return hashToDocFeatures.groupByKey()
				.flatMap(i -> reportFeatureSetCandidates(i, fingerprinter));
	}

	private static Iterator<FeatureSetCandidate> reportFeatureSetCandidates(Tuple2<String, Iterable<SimHashDocumentFeatures>> a, Fingerprinter<Integer> fingerprinter) {
		List<SimHashDocumentFeatures> docs = new ArrayList<>(ImmutableList.copyOf(a._2.iterator()));
		List<FeatureSetCandidate> ret = new ArrayList<>();
		
		for(int i=0; i< docs.size(); i++) {
			SimHashDocumentFeatures aFeatures = docs.get(i);
			
			byte[] hashA = HashTransformationUtil.integersToHash(aFeatures.simHash);
			for(int j=i+1; j<docs.size(); j++) {
				SimHashDocumentFeatures bFeatures = docs.get(j);
				
				if(!aFeatures.featureName.equals(bFeatures.featureName)) {
					continue;
				}
				
				byte[] hashB = HashTransformationUtil.integersToHash(bFeatures.simHash);
				
				int hemming = Hash.getHammingDistance(hashA, hashB);
				if(hemming <= 3) {
					ret.add(FeatureSetCandidate.featureSetCandidateOrNull(aFeatures, bFeatures));
				}
			}
		}
		
		return ret.stream().filter(i -> i != null).iterator();
	}


	public static JavaRDD<String> reportFeatureSetEvaluation(JavaRDD<String> input, Fingerprinter<Integer> fingerprinter, double threshold) {
		JavaRDD<FeatureSetCandidate> a = featureSetCandidates(input, fingerprinter);
		JavaRDD<FeatureSetCandidate> b = groundTruth(input, threshold);
		
		JavaPairRDD<Tuple2<String, String> ,String> ret = a.union(b).mapToPair(i -> new Tuple2<>(new Tuple2<>(i.firstId, i.secondId), i.featureName));
		
		return ret.groupByKey()
				.map(i -> reportEvaluationForFeatureSet(i));
	}
	
	@SneakyThrows
	private static String reportEvaluationForFeatureSet(Tuple2<Tuple2<String, String>, Iterable<String>> i) {
		Set<String> features = new HashSet<>(); 
		i._2.iterator().forEachRemaining(b -> features.add(b));
		List<String> tmp = new LinkedList<>(features);
		Collections.sort(tmp);
		
		Map<String, Object> ret = new LinkedHashMap<>();
		ret.put("firstId", i._1._1);
		ret.put("secondId", i._1._2);
		ret.put("featureNames", tmp);
		
		return new ObjectMapper().writeValueAsString(ret);
	}

	public static JavaRDD<FeatureSetCandidate> groundTruth(JavaRDD<String> input, double threshold) {
		return input.map(i -> groundTruthOrNull(i, threshold)).filter(i -> i != null);
	}
	
	public static JavaPairRDD<String, SimHashDocumentFeatures> featureHashToDocToFeatures(JavaRDD<String> input, Fingerprinter<Integer> fingerprinter) {
		JavaPairRDD<String, DocToFeatures> docToFeatures = input.flatMap(i -> extractPairs(i, fingerprinter))
				.mapToPair(i -> new Tuple2<>(i.docId, i));
		
		docToFeatures = docToFeatures.groupByKey()
			.mapToPair(i -> keepOnlyFirst(i));
		
		return docToFeatures.flatMapToPair(i -> extractAllFeatures(i._2())).filter(i -> i != null);
	}

	private static Iterator<Tuple2<String, SimHashDocumentFeatures>> extractAllFeatures(DocToFeatures i) {
		List<Tuple2<String, SimHashDocumentFeatures>> ret = new ArrayList<>();
		
		for(SimHashDocumentFeatures featureSet: i.features) {
			for(Integer feature: featureSet.simHash) {
				String key = featureSet.featureName +"___" + feature;
				ret.add(new Tuple2<>(key, featureSet));
			}
		}
		
		return ret.iterator();
	}

	private static Tuple2<String, DocToFeatures> keepOnlyFirst(Tuple2<String, Iterable<DocToFeatures>> i) {
		return new Tuple2<>(i._1(), i._2().iterator().next());
	}

	private static Iterator<DocToFeatures> extractPairs(String src, Fingerprinter<Integer> fingerprinter) {
		CanonicalLinkGraphEdge2 edge = CanonicalLinkGraphEdge2.fromString(src);
		
		return Arrays.asList(
				new DocToFeatures(edge.getFirstDoc().getDoc(), fingerprinter),
				new DocToFeatures(edge.getSecondDoc().getDoc(), fingerprinter)
		).iterator();
	}
	
	private static FeatureSetCandidate groundTruthOrNull(String src, double threshold) {
		CanonicalLinkGraphEdge2 edge = CanonicalLinkGraphEdge2.fromString(src);
		CollectionDocument a = edge.getFirstDoc().getDoc();
		CollectionDocument b = edge.getSecondDoc().getDoc();
		
		if(edge.getS3score() < threshold) {
			return null;
		}
		
		if(a.getId().compareTo(b.getId()) < 0) {
			return new FeatureSetCandidate("S3", a.getId(), b.getId());
		} else if (a.getId().compareTo(b.getId()) > 0) {
			return new FeatureSetCandidate("S3", b.getId(), a.getId());
		} else {
			return null;
		}
	}
	
	@Data
	@SuppressWarnings("serial")
	public static class SimHashDocumentFeatures implements Serializable {
		private final String featureName;
		private final String docId;
		private final List<Integer> simHash;
	}
	
	@Data
	@SuppressWarnings("serial")
	public static class DocToFeatures implements Serializable {
		private final String docId;
		private final List<SimHashDocumentFeatures> features;
		
		public DocToFeatures(CollectionDocument doc, Fingerprinter<Integer> fingerprinter) {
			this.docId = doc.getId();
			Map<String, List<String>> allFeatures = allFeatures(doc);
			features = new ArrayList<>();
			
			for(String featureName: allFeatures.keySet()) {
				features.add(new SimHashDocumentFeatures(featureName, doc.getId(), fingerprinter.fingerprint(allFeatures.get(featureName))));
			}
		}
	}
	
	@Data
	@SuppressWarnings("serial")
	public static class FeatureSetCandidate implements Serializable {
		private final String featureName;
		private final String firstId;
		private final String secondId;
		
		public static FeatureSetCandidate featureSetCandidateOrNull(SimHashDocumentFeatures a, SimHashDocumentFeatures b) {
			if(a == null || a.docId == null || b == null || b.docId == null || a.docId.compareTo(b.docId) == 0) {
				return null;
			}
			
			if(a.docId.compareTo(b.docId) < 0) {
				return new FeatureSetCandidate(a.featureName, a.docId, b.docId);
			} else if (a.docId.compareTo(b.docId) > 0) {
				return new FeatureSetCandidate(a.featureName, b.docId, a.docId);
			} else {
				return null;
			}
		}
	}
}
