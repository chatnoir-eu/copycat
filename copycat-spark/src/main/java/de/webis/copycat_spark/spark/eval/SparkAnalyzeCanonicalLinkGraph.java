package de.webis.copycat_spark.spark.eval;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.htrace.shaded.fasterxml.jackson.databind.ObjectMapper;
import org.apache.spark.HashPartitioner;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import de.webis.copycat_spark.spark.SparkCanonicalLinkGraphExtraction.CanonicalLinkGraphEdge;
import lombok.SneakyThrows;
import scala.Tuple2;

public class SparkAnalyzeCanonicalLinkGraph {

	private static final String DIR = "cikm2020/canonical-link-graph/";
	
	private static final String[] CORPORA = new String[] {/*"cw12", "cw09",*/ "cc-2015-11", "cc-2017-04"};
	
	public static void main(String[] args) {
		try (JavaSparkContext context = context()) {
			for(String corpus : CORPORA) {
				JavaRDD<String> input = context.textFile(DIR + corpus + "-calulated-edges-sampled-large-groups");
				Map<String, Long> edgeToCount = countEdgesAboveThreshold(input);
				
				context.parallelize(Arrays.asList(edgeToCount), 1)
					.map(i -> serialize(i))
					.saveAsTextFile(DIR + corpus + "-s3-edge-aggregations");
			}
		}
	}
	
//	public static void main(String[] args) {
//		try (JavaSparkContext context = context()) {
//			for(String corpus : CORPORA) {
//				JavaRDD<String> input = context.textFile(DIR + corpus);
//				
//				duplicateGroupCounts(input)
//					.saveAsTextFile(DIR + corpus + "-duplicate-group-counts");
//				duplicateGroupCountsPerDomain(input)
//					.saveAsTextFile(DIR + corpus + "-duplicate-group-counts-per-domain");
//			}
//		}
//	}
	
//	public static void main(String[] args) {
//		try (JavaSparkContext context = context()) {
//			for(String corpus : CORPORA) {
//				JavaRDD<String> input = context.textFile(DIR + corpus + "-canonical-urls");
//				
//				existingUrlToCount(input)
//					.saveAsTextFile(DIR + corpus + "-canonical-urls-to-count");
//			}
//		}
//	}
	
//	public static void main(String[] args) {
//		try (JavaSparkContext context = context()) {
//			for(String corpus : CORPORA) {
//				for(String sampleSize: new String[] {"0.05", "0.1", "0.15", "0.2"}) {
//					JavaRDD<String> input = context.textFile(DIR + corpus + "-canonical-urls-to-count")
//							.sample(Boolean.FALSE, Float.parseFloat(sampleSize), 1);
//					
//					input.saveAsTextFile(DIR + corpus + "-canonical-urls-to-count-sample-" + sampleSize);
//					JavaPairRDD<String, Integer> urlToCount = input.mapToPair(i -> toUrlToCount(i));
//					
//					JavaPairRDD<String, Integer> groupSizeToOne = urlToCount.mapToPair(i -> new Tuple2<>(i._2() + "", 1));
//	
//					groupSizeToOne.reduceByKey((a,b) -> a+b)
//							.map(i -> "{\"groupSize\": " + i._1() + ", \"count\": " + i._2() +"}")
//							.saveAsTextFile(DIR + corpus + "-duplicate-group-counts-sample-" + sampleSize);
//					
//					
//					urlToCount.mapToPair(i -> new Tuple2<String, Tuple2<Integer, Integer>>(hostFromUrl(i._1()), new Tuple2<>(i._2(), 1)))
//						.reduceByKey((a,b) -> new Tuple2<Integer, Integer>(a._1() + b._1(), a._2() + b._2()))
//						.map(i -> "{\"domain\":\"" + i._1() + "\",\"groups\":" + i._2()._2() + ",\"documents\":" + i._2()._1() + "}")
//						.saveAsTextFile(DIR + corpus + "-duplicate-group-counts-per-domain-sample-" + sampleSize);
//				}
//			}
//		}
//	}
	
	private static JavaSparkContext context() {
		SparkConf conf = new SparkConf(true);
		conf.setAppName("cikm2020/analyze-canonical-link-graph");
	
		return new JavaSparkContext(conf);
	}
	
	public static JavaRDD<String> duplicateGroupCounts(JavaRDD<String> input) {
		JavaPairRDD<String, Integer> groupSizeToOne = urlToCount(input)
				.mapToPair(i -> new Tuple2<>(i._2() + "", 1));

		return groupSizeToOne.reduceByKey((a,b) -> a+b)
				.map(i -> "{\"groupSize\": " + i._1() + ", \"count\": " + i._2() +"}");
	}

	private static JavaPairRDD<String, Integer> urlToCount(JavaRDD<String> input) {
		return input.map(i -> CanonicalLinkGraphEdge.fromString(i))
				.mapToPair(i -> new Tuple2<>(i.getCanonicalLink().toString(), 1))
//				.repartitionAndSortWithinPartitions(partitioner)
//				.persist(StorageLevel.DISK_ONLY())
				.reduceByKey((a,b) -> a+b)
				.filter(i -> i._2() > 1);
	}
	
//	public static void main(String[] args) {
//		try (JavaSparkContext context = context()) {
//			for(String corpus : CORPORA) {
//				JavaRDD<String> input = context.textFile(DIR + corpus);
//				
//				input.map(i -> CanonicalLinkGraphEdge.fromString(i).getCanonicalLink().toString())
//					.filter(i -> i != null)
//					.saveAsTextFile(DIR + corpus + "-canonical-urls");
//			}
//		}
//	}
	
	private static JavaRDD<String> existingUrlToCount(JavaRDD<String> input) {
		return input.mapToPair(i -> new Tuple2<>(i, 1))
				.repartitionAndSortWithinPartitions(new HashPartitioner(10000))
				.reduceByKey((a,b) -> a+b)
				.filter(i -> i._2() > 1)
				.map(i -> urlToCount(i._1(), i._2()));
	}
	
	@SneakyThrows
	private static String urlToCount(String url, int count) {
		Map<String, Object> ret = new LinkedHashMap<>();
		ret.put("url", url);
		ret.put("count", count);
		
		return new ObjectMapper().writeValueAsString(ret);
	}
	
	@SneakyThrows
	@SuppressWarnings("unchecked")
	private static Tuple2<String, Integer> toUrlToCount(String src) {
		Map<String, Object> ret = new ObjectMapper().readValue(src, Map.class);
		String url = (String) ret.get("url");
		Integer count = (Integer) ret.get("count");
		
		if(url == null || count == null || count <= 0) {
			throw new RuntimeException("Fix this");
		}
		
		return new Tuple2<>(url, count);
	}
	
	public static JavaRDD<String> duplicateGroupCountsPerDomain(JavaRDD<String> input) {
		return urlToCount(input)
				.mapToPair(i -> new Tuple2<String, Tuple2<Integer, Integer>>(hostFromUrl(i._1()), new Tuple2<>(i._2(), 1)))
				.reduceByKey((a,b) -> new Tuple2<Integer, Integer>(a._1() + b._1(), a._2() + b._2()))
				.map(i -> "{\"domain\":\"" + i._1() + "\",\"groups\":" + i._2()._2() + ",\"documents\":" + i._2()._1() + "}");
	}
	
	@SneakyThrows
	public static String hostFromUrl(String url) {
		String origUrl = url;
		url = url.replaceAll("\\s", "");
		try {
			return new URI(url).getHost();
		} catch (URISyntaxException e) {
			try {
				url = url.substring(0,  e.getIndex());
				return new URI(url).getHost();
			} catch (Exception e2) {
				System.out.println("ERROR-PARSING-URL: "+ origUrl);
				return "ERROR-PARSING-URL";
			}
		}
	}

	public static Map<String, Long> s3thresholdToEdgeCountNullValue() {
		Map<String, Long> ret = new LinkedHashMap<>();
		double current = 0d;
		
		while (current < 1d) {
			ret.put(String.format("%.4f", current), 0l);
			current += 0.005d;
		}
		
		return ret;
	}

	public static Map<String, Long> add(Map<String, Long> map, double d) {
		List<String> keys = new ArrayList<>(map.keySet());
		Collections.sort(keys, (a,b) -> Double.valueOf(b).compareTo(Double.valueOf(a)));
		
		for(String key : keys) {
			if(Double.valueOf(key) <= d) {
				map.put(key, map.get(key) + 1);
				break;
			}
		}
		
		return map;
	}

	public static Map<String, Long> merge(Map<String, Long> a, Map<String, Long> b) {
		Map<String, Long> ret = new LinkedHashMap<>();
		
		for(String key: a.keySet()) {
			ret.put(key, a.get(key) + b.get(key));
		}
		
		return ret;
	}

	public static Map<String, Long> countEdgesAboveThreshold(JavaRDD<String> input) {
		JavaRDD<Double> s3Scores = input.map(i -> extractS3Score(i));
		
		return s3Scores.aggregate(s3thresholdToEdgeCountNullValue(),
				(map, d) -> add(map, d), (map1, map2) -> merge(map1, map2));
	}
	
	@SneakyThrows
	@SuppressWarnings("unchecked")
	static Double extractS3Score(String src) {
		Map<String, Object> bla = new ObjectMapper().readValue(src, Map.class);
		Object ret = bla.get("s3score");
		
		if(ret instanceof Double) {
			return (Double) ret;
		} else if( ret instanceof Integer) {
			return ((Integer) ret).doubleValue();
		}
		
		return Double.valueOf((String) ret);
	}
	
	@SneakyThrows
	private static String serialize(Object o) {
		return new ObjectMapper().writeValueAsString(o);
	}
}
