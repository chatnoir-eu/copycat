package de.webis.copycat_spark.spark;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import de.webis.copycat_spark.util.ClientLocalDeduplication;
import de.webis.copycat_spark.util.ClientLocalDeduplication.DeduplicationTask;
import scala.Tuple2;

/**
 * 
 * @author Maik Fröbe
 *
 */
public class SparkDeduplicateCandidates {
//	public static void main(String[] args) {
//		String corpus = "cw09-cw12";
//		try (JavaSparkContext context = context()) {
//			context.textFile(inputPath(corpus))
//				.map(i-> DeduplicationTask.fromString(i))
//				.flatMap(i -> ClientLocalDeduplication.fullDeduplication(i.getEntries()).iterator())
//				.saveAsTextFile("cikm2020/deduplication-final/64BitK3SimHashThreeAndFiveGramms/" + corpus + "-near-duplicates-without-exact-duplicates");
//		}
//	}

//	public static void main(String[] args) {
//		//String corpus = "cw09-cw12-cc-2015-11";
//		String corpus = "cc-2017-04";
//		try (JavaSparkContext context = context()) {
//			for(int part=0; part<10; part++) {
//				context.textFile(inputPath(corpus, part))
//					.map(i-> DeduplicationTask.fromString(i))
//					.flatMap(i -> ClientLocalDeduplication.fullDeduplication(i.getEntries()).iterator())
//					.saveAsTextFile("cikm2020/deduplication-final/64BitK3SimHashThreeAndFiveGramms/" + corpus + "-near-duplicates-without-exact-duplicates/part-" + part);
//			}
//		}
//	}
	
	public static void main(String[] args) {
//		String corpus = "cw09";
		String corpus = "cw12";
		
		try (JavaSparkContext context = context()) {
			JavaRDD<String> input = context.textFile("cikm2020/deduplication-final/64BitK3SimHashOneGramms-canonical-urls/" + corpus + "-near-duplicate-tasks");
			deduplicateCandidates(input)				
				.saveAsTextFile("cikm2020/deduplication-final/64BitK3SimHashOneGramms-canonical-urls/" + corpus + "-near-duplicates");
		}
	}
	
	public static JavaRDD<String> deduplicateCandidates(JavaRDD<String> input) {
		return input.map(i-> DeduplicationTask.fromString(i))
				.flatMap(i -> ClientLocalDeduplication.fullDeduplication(i.getEntries()).iterator());
	}
	
//	public static void main(String[] args) {
//		String corpus = "cw09-cw12";
//		try (JavaSparkContext context = context()) {
//			JavaPairRDD<Integer, Integer> deduplicationTaskSizeToCount = context.textFile(inputPath(corpus))
//				.mapToPair(i-> new Tuple2<>(DeduplicationTask.fromString(i).getEntries().size(), 1))
//				.reduceByKey((a,b) -> a+b);
//			
//			deduplicationTaskSizeToCount
//				.map(i -> "{\"groupSize\":" + i._1() + ",\"count\":" + i._2() + "}")
//				.saveAsTextFile("cikm2020/document-fingerprints-final/recursively-blocked-deduplication-task-size-to-count/" + corpus);
//		}
//	}
	
	private static String inputPath(String corpus) {
		return "cikm2020/deduplication-final/64BitK3SimHashThreeAndFiveGramms/" + corpus +"-near-duplicate-tasks";
	}
	
	private static String inputPath(String corpus, int part) {
		return "cikm2020/deduplication-final/64BitK3SimHashThreeAndFiveGramms/" + corpus +"-near-duplicate-tasks/part-*" + part;
	}

	private static JavaSparkContext context() {
		SparkConf conf = new SparkConf(true);
		conf.setAppName("cikm2020/deduplication/near-duplicates");

		return new JavaSparkContext(conf);
	}
}
