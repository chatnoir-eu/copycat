package de.webis.cikm20_duplicates.spark;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.BZip2Codec;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaHadoopRDD;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.json.JSONException;
import org.json.JSONObject;

import de.webis.cikm20_duplicates.util.FingerPrintUtil.Fingerprinter;
import de.webis.cikm20_duplicates.util.FingerPrintUtil;
import de.webis.cikm20_duplicates.util.SourceDocuments;
import de.webis.cikm20_duplicates.util.SourceDocuments.CollectionDocumentWithTopics;
import de.webis.cikm20_duplicates.util.SourceDocuments.DocumentWithFingerprint;
import de.webis.trec_ndd.trec_collections.AnseriniCollectionReader;
import de.webis.trec_ndd.trec_collections.CollectionDocument;
import de.webis.trec_ndd.util.S3Files;
import io.anserini.index.transform.JsoupStringTransform;
import lombok.SneakyThrows;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import scala.Tuple2;
import de.webis.trec_ndd.trec_collections.CollectionConfiguration.OtherCollections;
import de.webis.trec_ndd.trec_collections.CollectionConfiguration.TrecCollections;

/**
 * 
 * @author Maik Fröbe
 *
 */
public class SparkCreateSourceDocuments {
	
	static final Map<String, Set<String>> DOCS_TO_TOPIC = docsToTopic();

	private static final String[] CORPORA = new String[] {/*"cw09", "cw12", "cc-2015-11", "cc-2017-04", */ "cc-2015-11-small-sample", "cc-2017-04-small-sample"};
	
	private static final AnseriniCollectionReader<?>
			CLUEWEB09 = new AnseriniCollectionReader<>(TrecCollections.CLUEWEB09),
			CLUEWEB12 = new AnseriniCollectionReader<>(TrecCollections.CLUEWEB12);
	
	public static final List<Fingerprinter<Integer>> PRODUCTION_FINGERPRINTS = Arrays.asList(
		FingerPrintUtil.simHashFingerPrinting(64, 3),
		FingerPrintUtil.productionFingerpringint(64, 3)	
	);

	public static void main(String[] args) {
		try (JavaSparkContext context = context()) {
			for(String corpus: CORPORA) {
				JavaRDD<CollectionDocument> docs = docs(context, corpus);
				
				fingerprintAllDocuments(context, docs, PRODUCTION_FINGERPRINTS)
					.repartition(10000)
					.saveAsTextFile("cikm2020/document-fingerprints-final/" + corpus +"-jsonl.bzip2", BZip2Codec.class);
			}
		}
	}
	
	private static JavaRDD<CollectionDocument> docs(JavaSparkContext context, String corpus) {
		if ("cw09".equals(corpus)) {
			return docs(context, CLUEWEB09);
		} else if ("cw12".equals(corpus)) {
			return docs(context, CLUEWEB12);
		} else if ("cc-2015-11".equals(corpus)) {
			return ccDocs(context, "/corpora/corpus-commoncrawl/CC-MAIN-2015-11-mapfile/data-r-*/data");
		} else if ("cc-2017-04".equals(corpus)) {
			return ccDocs(context, "/corpora/corpus-commoncrawl/CC-MAIN-2017-04-mapfile/data-r-*/data");
		} else if ("cc-2015-11-small-sample".equals(corpus)) {
			return ccDocsWithRepartition(context, "/corpora/corpus-commoncrawl/CC-MAIN-2015-11-mapfile/data-r-00001/data");
		} else if ("cc-2017-04-small-sample".equals(corpus)) {
			return ccDocsWithRepartition(context, "/corpora/corpus-commoncrawl/CC-MAIN-2017-04-mapfile/data-r-00001/data");
		} else {
			throw new RuntimeException("Add more corpora");
		}
	}

	private static JavaSparkContext context() {
		SparkConf conf = new SparkConf(true);
		conf.setAppName("cikm2020/source-documents");

		return new JavaSparkContext(conf);
	}
	
	public static JavaRDD<CollectionDocumentWithTopics> transformAllImportantDocuments(JavaSparkContext context, AnseriniCollectionReader<?>...acrs) {
		return docs(context, acrs)
				.map(doc -> transformDocIfImportantOrNull(doc))
				.filter(i -> i != null); 
	}
	
	public static JavaRDD<DocumentWithFingerprint> fingerprintAllDocuments(JavaSparkContext context, JavaRDD<CollectionDocument> docs, List<Fingerprinter<Integer>> fingerprinters) {
		return docs.map(i -> new DocumentWithFingerprint(i.getId(), i.getUrl(), i.getCanonicalUrl(), fp(i, fingerprinters)));
	}
	
	private static LinkedHashMap<String, ArrayList<Integer>> fp(CollectionDocument doc, List<Fingerprinter<Integer>> fingerprinters) {
		LinkedHashMap<String, ArrayList<Integer>> ret = new LinkedHashMap<>();
		
		for(Fingerprinter<Integer> f: fingerprinters) {
			ret.put(f.fingerprinterName(), new ArrayList<>(f.fingerprint(doc)));
		}
		
		return ret;
	}
	
	@SuppressWarnings("rawtypes")
	public static JavaRDD<DocumentWithFingerprint> fingerprintAllDocuments(JavaSparkContext context, List<Fingerprinter<Integer>> fingerprinters, AnseriniCollectionReader...acr) {
		return fingerprintAllDocuments(context, docs(context, acr), fingerprinters);
	}
	
	@SuppressWarnings("unchecked")
	static JavaRDD<CollectionDocument> ccDocsWithRepartition(JavaSparkContext context, String path) {
		JavaPairRDD<String, String> rdd = ((JavaHadoopRDD<Text, Text>) context.hadoopFile(path, SequenceFileInputFormat.class, Text.class, Text.class))
				.mapToPair(kv -> new Tuple2<>(kv._1().toString(), kv._2().toString()));
		rdd = rdd.repartition(100);
		
		return ccDocs(rdd);
	}
	
	@SuppressWarnings("unchecked")
	static JavaRDD<CollectionDocument> ccDocs(JavaSparkContext context, String path) {
		JavaHadoopRDD<Text, Text> rdd = (JavaHadoopRDD<Text, Text>) context.hadoopFile(path, SequenceFileInputFormat.class, Text.class, Text.class);
		
		return ccDocs(rdd.mapToPair(kv -> new Tuple2<>(kv._1().toString(), kv._2().toString())));
	}
	
	static JavaRDD<CollectionDocument> ccDocs(JavaPairRDD<String, String> rdd) {
		return rdd.map(kv -> chatnoirMapFileDocumentToDocOrNull(kv._1(), kv._2()))
				.filter(i -> i != null);
	}

	@SneakyThrows
	private static CollectionDocument chatnoirMapFileDocumentToDocOrNull(String keyStr, String valueStr) {
		// ignore large files
		if (valueStr.getBytes().length > 1024 * 1024) {
			return null;
		}

		JSONObject inputJson  = new JSONObject(valueStr);

		final JSONObject metadata = inputJson.getJSONObject("metadata");
		if (null == metadata) {
			throw new JSONException("Missing 'metadata'");
		}

		final JSONObject payload = inputJson.getJSONObject("payload");
		if (null == payload) {
			throw new JSONException("Missing 'payload'");
		}

		final String contentBody = payload.getString("body");
		final String contentEncoding    = payload.getString("encoding");

		if (null == contentEncoding || null == contentBody) {
			throw new JSONException("Missing one of 'payload/[encoding|body]'");
		}

		if (!contentEncoding.equals("plain")) {
			return null;
		}

		if(!"response".equals(metadata.getString("WARC-Type"))) {
			return null;
		}

		String targetUri = metadata.getString("WARC-Target-URI");
		String recordId = metadata.getString("WARC-Record-ID");

		CollectionDocument ret = CollectionDocument.collectionDocument(new JsoupStringTransform().apply(contentBody), keyStr);
		ret.setUrl(new URL(targetUri));
		ret.setCanonicalUrl(SparkCanonicalLinkGraphExtraction.extractCanonicalLinkOrNull(targetUri, contentBody));

		return ret;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static JavaRDD<CollectionDocument> docs(JavaSparkContext context, AnseriniCollectionReader...acrs) {
		List<Tuple2<AnseriniCollectionReader, String>> readerToSegment = new ArrayList<>();
		
		for(AnseriniCollectionReader acr: acrs) {
			List<String> segmentPaths = acr.segmentPaths();
			for(String segmentPath: segmentPaths) {
				readerToSegment.add(new Tuple2<>(acr, segmentPath));
			}
		}
		
		return context.parallelize(readerToSegment, readerToSegment.size())
				.flatMap(i -> i._1().collectionDocumentsInPath(i._2()));
	}
	
	private static CollectionDocumentWithTopics transformDocIfImportantOrNull(CollectionDocument doc) {
		if(!DOCS_TO_TOPIC.containsKey(doc.getId())) {
			return null;
		}
		
		return new CollectionDocumentWithTopics(doc, new ArrayList<>(DOCS_TO_TOPIC.get(doc.getId())));
	}
	
	private static Map<String, Set<String>> docsToTopic() {
		Map<String, Set<String>> ret = new HashMap<>();
		for(Map.Entry<String, Set<String>> topicToIds : SourceDocuments.topicsToDocumentIds().entrySet()) {
			String topic = topicToIds.getKey();
			for(String docId: topicToIds.getValue()) {
				if(!ret.containsKey(docId)) {
					ret.put(docId, new HashSet<>());
				}
				
				ret.get(docId).add(topic);
			}
		}
		
		return ret;
	}
	
	private static AnseriniCollectionReader<?> commonCrawl(String[] args) {
		Namespace parsedArgs = parseArguments(args);
		S3Files s3Files = new S3Files(
				parsedArgs.getString("accessKey"),
				parsedArgs.getString("secretKey"),
				"corpus-commoncrawl-main-2015-11"
		);
		
		OtherCollections config = OtherCollections.commonCrawl_2015_02(s3Files);
		return new AnseriniCollectionReader<>(config);
	}
	
	private static Namespace parseArguments(String[] args) {
		ArgumentParser parser = ArgumentParsers.newFor("SparkCreateSourceDocuments").build().defaultHelp(true);

		parser.addArgument("--accessKey")
			.required(Boolean.TRUE)
			.help("Specify the s3 access key.");
		parser.addArgument("--secretKey")
			.required(Boolean.TRUE)
			.help("Specify the s3 secret key.");
		
		return parser.parseArgsOrFail(args);
	}
}
