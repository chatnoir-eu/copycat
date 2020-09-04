package de.webis.cikm20_duplicates.app;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.compress.BZip2Codec;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.jsoup.Jsoup;

import de.webis.chatnoir2.mapfile_generator.warc.WarcRecord;
import de.webis.trec_ndd.trec_collections.CollectionDocument;
import lombok.SneakyThrows;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class ExtractHealthMisinformationDocuments {
	public static void main(String[] args) {
		Namespace parsedArgs = validArgumentsOrNull(args);
		
		if(parsedArgs == null) {
			return;
		}
		
		try (JavaSparkContext context = context()) {
			Set<String> idsToKeep = idsToKeep();
			JavaPairRDD<LongWritable, WarcRecord> records = WARCParsingUtil.records(context, parsedArgs);
			JavaRDD<CollectionDocument> parsedDocuments = records.map(i -> transformToCollectionDocument(i._2()))
					.filter(i -> i != null)
					.map(i -> fixId(i));
			
			parsedDocuments = parsedDocuments.filter(i -> idsToKeep.contains(i.getId()));

			parsedDocuments.filter(i -> i != null).map(i -> i.toString())
					.repartition(50)
					.saveAsTextFile(parsedArgs.getString(ArgumentParsingUtil.ARG_OUTPUT), BZip2Codec.class);
		}
	}
	
	private static CollectionDocument transformToCollectionDocument(WarcRecord record) {
		CollectionDocument doc = CreateDocumentRepresentations.transformToCollectionDocument(record);
		if(doc != null) {
			return doc;
		}
		
		Map<String, String> header = CreateDocumentRepresentations.lowercasedHeaders(record);
		String contentBody = record.getContent();
		
		String id = header.get("warc-trec-id");
		if (id == null || id.isEmpty()) {
			id = header.get("warc-record-id");
		}
		
		CollectionDocument ret = CollectionDocument.collectionDocument(Jsoup.parse(contentBody).text(), id);
		ret.setCrawlingTimestamp(header.get("warc-date"));
		
		return ret;
	}
	
	private static JavaSparkContext context() {
		SparkConf conf = new SparkConf(true);
		conf.setAppName("Health-Misinformation-Documents");

		return new JavaSparkContext(conf);
	}
	
	public static CollectionDocument fixId(CollectionDocument doc) {
		doc.setId(StringUtils.substringBefore(StringUtils.substringAfter(doc.getId(), "<urn:uuid:"), ">"));
		
		return doc;
	}
	
	@SneakyThrows
	public static Set<String> idsToKeep() {
		List<String> lines = IOUtils.readLines(ExtractHealthMisinformationDocuments.class.getResourceAsStream("/misinfo2020-pools"), StandardCharsets.UTF_8);
		
		return new HashSet<>(lines.stream().map(i -> StringUtils.substringAfter(i, " ")).collect(Collectors.toSet()));
	}
	
	static Namespace validArgumentsOrNull(String[] args) {
		ArgumentParser parser = argParser();

		try {
			return parser.parseArgs(args);
		} catch (ArgumentParserException e) {
			parser.handleError(e);
			return null;
		}
	}

	private static ArgumentParser argParser() {
		ArgumentParser ret = ArgumentParsers.newFor(ArgumentParsingUtil.TOOL_NAME + ": CreateDocumentRepresentations")
				.addHelp(Boolean.TRUE).build();

		ret.addArgument("-i", "--" + ArgumentParsingUtil.ARG_INPUT).required(Boolean.TRUE).help(
				"The input path that is passed to JavaSparkContext.hadoopFile to extract Documents from warc files. E.g. 's3a://corpus-clueweb09'.");

		ret.addArgument("-o", "--" + ArgumentParsingUtil.ARG_OUTPUT).required(Boolean.TRUE)
				.help("The resulting document representations are stored under this location.");

		ret.addArgument("-f", "--" + ArgumentParsingUtil.ARG_FORMAT).required(Boolean.TRUE)
				.choices(ArgumentParsingUtil.InputFormats.allInputFormats());

		return ret;
	}
}
