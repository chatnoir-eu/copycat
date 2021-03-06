create-documents-for-daniel: install
	./src/main/bash/run-spark-job.sh de.webis.copycat_spark.app.InjectRawDocuments

common-crawl-small-sample-document-representations: install
	hdfs dfs -rm -r -f corpus-copycat/document-representations/small-sample && \
	./src/main/bash/new-document-representation-spark.sh \
		--inputFormat COMMON_CRAWL \
		--input s3a://corpus-commoncrawl-main-2020-16/crawl-data/CC-MAIN-2020-16/segments/1585371896913.98/warc/CC-MAIN-20200410110538-20200410141038-005{1,2,3,4,5}*.warc.gz \
		--output corpus-copycat/document-representations/small-sample

clueweb09-document-representations: install
	hdfs dfs -rm -r -f ecir2021/cw09 && \
	./src/main/bash/new-document-representation-spark.sh \
		--inputFormat CLUEWEB09 \
		--input s3a://corpus-clueweb09/parts/*/*/*.warc.gz \
		--output ecir2021/cw09 && \
	hdfs dfs -rm -r -f ecir2021/cw09-repartitioned && \
	src/main/bash/repartition.sh \
		--input ecir2021/cw09 \
		--output ecir2021/cw09-repartitioned \
		--partitions 10000

clueweb09-main-content-document-representations: install
	hdfs dfs -rm -r -f ecir2021/cw09-main-content-extraction && \
	./src/main/bash/new-document-representation-spark.sh \
		--inputFormat CLUEWEB09 \
		--input s3a://corpus-clueweb09/parts/*/*/*.warc.gz \
		--mainContentExtraction true \
		--output ecir2021/cw09-main-content-extraction

clueweb12-document-representations: install
	hdfs dfs -rm -r -f ecir2021/cw12 && \
	./src/main/bash/new-document-representation-spark.sh \
		--inputFormat CLUEWEB12 \
		--input s3a://corpus-clueweb12/parts/*/*/*/*.warc.gz \
		--output ecir2021/cw12 && \
	hdfs dfs -rm -r -f ecir2021/cw12-repartitioned && \
	src/main/bash/repartition.sh \
		--input ecir2021/cw12 \
		--output ecir2021/cw12-repartitioned \
		--partitions 10000

clueweb12-main-content-document-representations: install
	hdfs dfs -rm -r -f ecir2021/cw12-main-content-extraction && \
	./src/main/bash/new-document-representation-spark.sh \
		--inputFormat CLUEWEB12 \
		--input s3a://corpus-clueweb12/parts/*/*/*/*.warc.gz \
		--mainContentExtraction true \
		--output ecir2021/cw12-main-content-extraction

common-crawl15-document-representations: install
	hdfs dfs -rm -r -f ecir2021/cc-2015-11 && \
	./src/main/bash/cc15-document-representations.sh 0 && \
	./src/main/bash/cc15-document-representations.sh 1 && \
	./src/main/bash/cc15-document-representations.sh 2 && \
	./src/main/bash/cc15-document-representations.sh 3 && \
	./src/main/bash/cc15-document-representations.sh 4 && \
	./src/main/bash/cc15-document-representations.sh 5 && \
	./src/main/bash/cc15-document-representations.sh 6 && \
	./src/main/bash/cc15-document-representations.sh 7 && \
	./src/main/bash/cc15-document-representations.sh 8 && \
	./src/main/bash/cc15-document-representations.sh 9 && \
	hdfs dfs -rm -r -f ecir2021/cc-2015-11-repartitioned && \
	src/main/bash/repartition.sh \
		--input ecir2021/cc-2015-11/*/ \
		--output ecir2021/cc-2015-11-repartitioned \
		--partitions 10000

common-crawl17-document-representations: install
	hdfs dfs -rm -r -f ecir2021/cc-2017-04 && \
	./src/main/bash/cc17-document-representations.sh 0 && \
	./src/main/bash/cc17-document-representations.sh 1 && \
	./src/main/bash/cc17-document-representations.sh 2 && \
	./src/main/bash/cc17-document-representations.sh 3 && \
	./src/main/bash/cc17-document-representations.sh 4 && \
	./src/main/bash/cc17-document-representations.sh 5 && \
	./src/main/bash/cc17-document-representations.sh 6 && \
	./src/main/bash/cc17-document-representations.sh 7 && \
	./src/main/bash/cc17-document-representations.sh 8 && \
	./src/main/bash/cc17-document-representations.sh 9 && \
	hdfs dfs -rm -r -f ecir2021/cc-2017-04-repartitioned && \
	src/main/bash/repartition.sh \
		--input ecir2021/cc-2017-04/*/ \
		--output ecir2021/cc-2017-04-repartitioned \
		--partitions 10000

label-data-daniel:
	./src/main/bash/label-data-canonical-edges-daniel.sh

repartition-parts: install
	./src/main/bash/run-spark-job.sh de.webis.copycat_spark.spark.util.SparkRepartitionParts

report-feature-sets: install
	./src/main/bash/run-spark-job.sh de.webis.copycat_spark.spark.eval.SparkEvaluateSimHashFeatures

prepare-precision-experiments: install
	./src/main/bash/run-spark-job-low-resources-akbnq.sh de.webis.copycat_spark.spark.eval.SparkCalculatePrecisionInCanonicalLinkGraph

report-precision-experiments: install
	./src/main/bash/run-spark-job.sh de.webis.copycat_spark.spark.eval.SparkCalculatePrecisionInCanonicalLinkGraph

sample-canonical-link-graph-edges: install
	./src/main/bash/run-spark-job.sh de.webis.copycat_spark.spark.eval.SparkSampleS3EdgesPerBin

corpus-analysis: install
	./src/main/bash/run-spark-job-low-resources-akbnq-new.sh de.webis.copycat_spark.spark.eval.SparkCorpusAnalysis

distinct-deduplication-pairs: install
	./src/main/bash/run-spark-job-low-resources-akbnq-new.sh de.webis.copycat_spark.spark.SparkMakeDeduplicatedPairsUnique

distinct-deduplication-pairs-onegramms: install 
	./src/main/bash/distinct-near-duplicate-pairs.sh \
		-i ecir2021/cw09-cw12-cc15-onegram-deduplication/min-length-10-/64BitK3SimHashOneGramms/near-duplicates \
		-o ecir2021/cw09-cw12-cc15-onegram-deduplication/min-length-10-/64BitK3SimHashOneGramms/distinct-near-duplicates

deduplication-pairs-between-corpora-onegramms: install
	hdfs dfs -rm -r -f  ecir2021/cw09-cw12-cc15-onegram-deduplication/min-length-10-/64BitK3SimHashOneGramms/distinct-near-duplicates-between-corpora && \
	./src/main/bash/deduplication-pairs-between-corpora.sh \
		-i ecir2021/cw09-cw12-cc15-onegram-deduplication/min-length-10-/64BitK3SimHashOneGramms/distinct-near-duplicates \
		-o ecir2021/cw09-cw12-cc15-onegram-deduplication/min-length-10-/64BitK3SimHashOneGramms/distinct-near-duplicates-between-corpora

create-ids-to-remove: install
	./src/main/bash/run-spark-job.sh de.webis.copycat_spark.spark.SparkCreateIdsToRemove

report-short-documents: install
	./src/main/bash/run-spark-job.sh de.webis.copycat_spark.spark.eval.SparkAnalyzeShortDocuments

analize-tmp: install
	./src/main/bash/run-spark-job.sh de.webis.copycat_spark.spark.eval.SparkAnalyzeCanonicalLinkGraph

report-document-lengths: install
	./src/main/bash/run-low-resource-spark-job.sh de.webis.copycat_spark.spark.eval.SparkAnalyzeDocumentLength
	
s3-score-per-hamming: install
	./src/main/bash/run-low-resource-spark-job.sh de.webis.copycat_spark.spark.eval.SparkAnalyzeAverageS3ScorePerHammingDistance

create-canonical-link-graph-edges: install
	./src/main/bash/run-spark-job.sh de.webis.copycat_spark.spark.SparkCalculateCanonicalLinkGraphEdgeLabels

analyze-canonical-link-graph: install
	hdfs dfs -rm -R -f cikm2020/canonical-link-graph/cw12-duplicate-group-counts && \
	hdfs dfs -rm -R -f cikm2020/canonical-link-graph/cw12-duplicate-group-counts-per-domain && \
	hdfs dfs -rm -R -f cikm2020/canonical-link-graph/cw09-duplicate-group-counts && \
	hdfs dfs -rm -R -f cikm2020/canonical-link-graph/cw09-duplicate-group-counts-per-domain && \
	hdfs dfs -rm -R -f cikm2020/canonical-link-graph/cc-2015-11-duplicate-group-counts && \
	hdfs dfs -rm -R -f cikm2020/canonical-link-graph/cc-2015-11-duplicate-group-counts-per-domain && \
	./src/main/bash/run-spark-job.sh de.webis.copycat_spark.spark.eval.SparkAnalyzeCanonicalLinkGraph

create-canonical-link-graph: install
	./src/main/bash/run-spark-job.sh de.webis.copycat_spark.spark.SparkCanonicalLinkGraphExtraction

relevance-transfer: install
	./src/main/bash/run-spark-job.sh de.webis.copycat_spark.spark.eval.SparkAggregateKnowledgeTransferBetweenCrawls

relevance-transfer-ids: install
	./src/main/bash/run-spark-job.sh de.webis.copycat_spark.spark.eval.SparkCreateTargetDocumentsForRelevanceTransfer

crawl-containment: install
	./src/main/bash/run-spark-job.sh de.webis.copycat_spark.spark.eval.SparkIdentifyDocumentsInTargetCrawl

enrich-relevance-transfer-dataset: install
	hdfs dfs -rm -r -f cikm2020/enriched-relevance-transfer-pairs.jsonl && \
	./src/main/bash/run-spark-job.sh de.webis.copycat_spark.spark.SparkEnrichRelevanceTransferPairs

create-relevance-transfer-dataset: install
	hdfs dfs -rm -r -f cikm2020/relevance-transfer-pairs.jsonl && \
	./src/main/bash/run-spark-job.sh de.webis.copycat_spark.spark.SparkRelevanceTransferDataConstruction

count-edges: install
	hdfs dfs -rm -r -f cikm2020/near-duplicate-graph/edge-count-cw09-cw12 && \
	./src/main/bash/run-spark-job.sh de.webis.copycat_spark.spark.SparkCountEdgeLabels

combine-intermediate-results: install
	hdfs dfs -rm -r -f cikm2020/results/test-01 && \
	./src/main/bash/run-spark-job.sh de.webis.copycat_spark.spark.SparkCombineIntermediateResults

deduplicate-cw09: install
	src/main/bash/deduplicate.sh \
		--input ecir2021/cw09-deduplication/min-length-10-near-duplicate-tasks \
		--output ecir2021/cw09-deduplication/min-length-10-near-duplicates

deduplicate-cw12: install
	src/main/bash/deduplicate.sh \
		--input ecir2021/cw12-deduplication/min-length-10-near-duplicate-tasks \
		--output ecir2021/cw12-deduplication/min-length-10-near-duplicates

deduplicate-cw09-cw12-cc15: install
	src/main/bash/deduplicate.sh \
		--input ecir2021/cw09-cw12-cc15-deduplication/min-length-10-near-duplicate-tasks \
		--output ecir2021/cw09-cw12-cc15-deduplication/min-length-10-near-duplicates

deduplicate-cc15: install
	src/main/bash/deduplicate.sh \
		--input ecir2021/cc-2015-11-deduplication/min-length-10-near-duplicate-tasks \
		--output ecir2021/cc-2015-11-deduplication/min-length-10-near-duplicates

deduplicate-cc17: install
	src/main/bash/deduplicate.sh \
		--input ecir2021/cc-2017-04-deduplication/min-length-10-near-duplicate-tasks \
		--output ecir2021/cc-2017-04-deduplication/min-length-10-near-duplicates

sample-near-duplicates-cw09: install
	hdfs dfs -rm -r -f ecir2021/cw09-deduplication/sample-near-duplicates-min-length-10.jsonl && \
	src/main/bash/sample-near-duplicates.sh \
		--input ecir2021/cw09-deduplication/min-length-10 \
		--num 10000 \
		--output ecir2021/cw09-deduplication/sample-near-duplicates-min-length-10.jsonl \
		--uuidIndex cw09 \
		--uuidPrefix clueweb09

enrich-near-duplicate-pairs-with-judged-documents: install
	hdfs dfs -rm -r -f ecir2021/cw09-deduplication/tmp-cw09-enriched-near-duplicate-pairs && \
	src/main/bash/enrich-near-duplicates-with-s3-scores.sh \
		--input ecir2021/cw09-cw12-cc15-onegram-deduplication/min-length-10-/64BitK3SimHashOneGramms/distinct-near-duplicates-between-corpora/with-judgments-in-web-track \
		--output ecir2021/cw09-cw12-cc15-onegram-deduplication/min-length-10-/64BitK3SimHashOneGramms/relevance-transfer-near-duplicates

small-test-enrich-near-duplicates: install
	./src/main/bash/run-in-docker-container-with-spark.sh hdfs dfs -rm -r -f sigir21/enrichment-cw09-cw12-pairs/part-1 && \
	./src/main/bash/enrich-near-duplicates-with-s3-scores.sh \
		--input /corpora/corpus-copycat/deduplication-final/64BitK3SimHashThreeAndFiveGramms/cw09-cw12-near-duplicates-without-exact-duplicates-csv-distinct/*1 \
		--output sigir21/enrichment-cw09-cw12-pairs/part-1 \
		--inputFormat csv

sigir21-cw09-hashes: install
	hdfs dfs -rm -r -f sigir21/doc-hash-cw09 && \
	./src/main/bash/document-hashes-spark.sh \
		--inputFormat CLUEWEB09 \
		--input s3a://corpus-clueweb09/parts/*/*/*.warc.gz \
		--output sigir21/doc-hash-cw09 && \
	hdfs dfs -rm -r -f sigir21/doc-hash-cw09-repartitioned && \
	src/main/bash/repartition.sh \
		--input sigir21/doc-hash-cw09 \
		--output sigir21/doc-hash-cw09-repartitioned \
		--partitions 10000

sigir21-cw12-hashes: install
	./src/main/bash/document-hashes-spark.sh \
		--inputFormat CLUEWEB12 \
		--input /corpora/corpora-thirdparty/corpus-clueweb/12/*/*/*/*.warc.gz \
		--output sigir21/doc-hash-cw12 && \
	src/main/bash/repartition.sh \
		--input sigir21/doc-hash-cw12 \
		--output sigir21/doc-hash-cw12-repartitioned \
		--partitions 10000

sigir21-cw09b-hashes:
	hdfs dfs -rm -r -f sigir21/doc-hash-cw09b && \
	./src/main/bash/document-hashes-spark.sh \
		--inputFormat CLUEWEB09 \
		--input /corpora/corpora-thirdparty/corpus-clueweb/09/ClueWeb09_English_1/*/*.warc.gz \
		--output sigir21/doc-hash-cw09b

sigir21-enrich-near-duplicates-0-9: install
	for I in $(seq -f "%02g" 0 9); do ./src/main/bash/sigir21-enrich.sh ${I}; done

sigir21-enrich-near-duplicates-10-20: install
	for I in $(seq 10 20); do ./src/main/bash/sigir21-enrich.sh ${I}; done

sigir21-enrich-near-duplicates-21-30: install
	for I in $(seq 21 30); do ./src/main/bash/sigir21-enrich.sh ${I}; done

exact-duplicates-between-corpora-for-relevance-transfer: install
	./src/main/bash/part-enrich-near-duplicates-with-s3-scores.sh 0 && \
	./src/main/bash/part-enrich-near-duplicates-with-s3-scores.sh 1 && \
	./src/main/bash/part-enrich-near-duplicates-with-s3-scores.sh 2 && \
	./src/main/bash/part-enrich-near-duplicates-with-s3-scores.sh 3 && \
	./src/main/bash/part-enrich-near-duplicates-with-s3-scores.sh 4 && \
	./src/main/bash/part-enrich-near-duplicates-with-s3-scores.sh 5 && \
	./src/main/bash/part-enrich-near-duplicates-with-s3-scores.sh 6 && \
	./src/main/bash/part-enrich-near-duplicates-with-s3-scores.sh 7 && \
	./src/main/bash/part-enrich-near-duplicates-with-s3-scores.sh 8 && \
	./src/main/bash/part-enrich-near-duplicates-with-s3-scores.sh 9

sample-near-duplicates-cw12: install
	hdfs dfs -rm -r -f ecir2021/cw12-deduplication/sample-near-duplicates-min-length-10.jsonl && \
	src/main/bash/sample-near-duplicates.sh \
		--input ecir2021/cw12-deduplication/min-length-10 \
		--num 10000 \
		--output ecir2021/cw12-deduplication/sample-near-duplicates-min-length-10.jsonl \
		--uuidIndex cw12 \
		--uuidPrefix clueweb12
exact-duplicates-between-corpora-for-relevance-transfer: install
	./src/main/bash/part-enrich-near-duplicates-with-s3-scores.sh 0 && \
	./src/main/bash/part-enrich-near-duplicates-with-s3-scores.sh 1 && \
	./src/main/bash/part-enrich-near-duplicates-with-s3-scores.sh 2 && \
	./src/main/bash/part-enrich-near-duplicates-with-s3-scores.sh 3 && \
	./src/main/bash/part-enrich-near-duplicates-with-s3-scores.sh 4 && \
	./src/main/bash/part-enrich-near-duplicates-with-s3-scores.sh 5 && \
	./src/main/bash/part-enrich-near-duplicates-with-s3-scores.sh 6 && \
	./src/main/bash/part-enrich-near-duplicates-with-s3-scores.sh 7 && \
	./src/main/bash/part-enrich-near-duplicates-with-s3-scores.sh 8 && \
	./src/main/bash/part-enrich-near-duplicates-with-s3-scores.sh 9

sample-near-duplicates-cw12: install
	hdfs dfs -rm -r -f ecir2021/cw12-deduplication/sample-near-duplicates-min-length-10.jsonl && \
	src/main/bash/sample-near-duplicates.sh \
		--input ecir2021/cw12-deduplication/min-length-10 \
		--num 10000 \
		--output ecir2021/cw12-deduplication/sample-near-duplicates-min-length-10.jsonl \
		--uuidIndex cw12 \
		--uuidPrefix clueweb12

sample-near-duplicates-cc17: install
	hdfs dfs -rm -r -f ecir2021/cc-2017-04-deduplication/sample-near-duplicates-min-length-10.jsonl && \
	src/main/bash/sample-near-duplicates.sh \
		--input ecir2021/cc-2017-04-deduplication/min-length-10 \
		--num 10000 \
		--output ecir2021/cc-2017-04-deduplication/sample-near-duplicates-min-length-10.jsonl \
		--uuidIndex cc1704 \
		--uuidPrefix commoncrawl

sample-near-duplicates-cc15: install
	hdfs dfs -rm -r -f ecir2021/cc-2015-11-deduplication/sample-near-duplicates-min-length-10.jsonl && \
	src/main/bash/sample-near-duplicates.sh \
		--input ecir2021/cc-2015-11-deduplication/min-length-10 \
		--num 10000 \
		--output ecir2021/cc-2015-11-deduplication/sample-near-duplicates-min-length-10.jsonl \
		--uuidIndex cc1511 \
		--uuidPrefix commoncrawl

deduplication-task-sizes: install
	./src/main/bash/run-low-resource-spark-job.sh de.webis.copycat_spark.spark.eval.SparkAnalyzeDeduplicationTaskSizes

create-deduplication-candidates-cw09: install
	src/main/bash/create-deduplication-candidates.sh \
		--input ecir2021/cw09-repartitioned/ \
		--output ecir2021/cw09-deduplication/ \
		--minimumDocumentLength 10

create-deduplication-candidates-cw12: install
	src/main/bash/create-deduplication-candidates.sh \
		--input ecir2021/cw12-repartitioned/ \
		--output ecir2021/cw12-deduplication/ \
		--minimumDocumentLength 10

create-deduplication-candidates-cc15: install
	src/main/bash/create-deduplication-candidates.sh \
		--input ecir2021/cc-2015-11-repartitioned/ \
		--output ecir2021/cc-2015-11-deduplication/ \
		--minimumDocumentLength 10

create-deduplication-candidates-cw09-cw12-cc15: install
	hdfs dfs -rm -r -f ecir2021/cw09-cw12-cc15-deduplication &&\
	src/main/bash/create-deduplication-candidates.sh \
		--input ecir2021/{cw09,cw12,cc-2015-11}-repartitioned/ \
		--output ecir2021/cw09-cw12-cc15-deduplication/ \
		--minimumDocumentLength 10

create-deduplication-candidates-cc17: install
	src/main/bash/create-deduplication-candidates.sh \
		--input ecir2021/cc-2017-04-repartitioned/ \
		--output ecir2021/cc-2017-04-deduplication/ \
		--minimumDocumentLength 10

create-url-candidates: install
	./src/main/bash/run-spark-job.sh de.webis.copycat_spark.spark.SparkCreateCanonicalLinkDeduplicationTasks

repartition-url-candidates-cw09-cw12-cc15: install
	./src/main/bash/repartition-url-candidates.sh \
		--input s3a://corpus-copycat/document-representations/{cw09,cw12,cc-2015-11}/*.bz2 \
		--output sigir21/docs-in-cw09-cw12-cc15-repartitioned-by-url

url-deduplication-candidates: install
	./src/main/bash/create-candidates-for-url-candidates.sh \
		--input sigir21/docs-in-cw09-cw12-cc15-repartitioned-by-url/*0.bz2 \
		--output sigir21/url-deduplication-cw09-cw12-cc15/part0 \
		--partitions 1000 

deduplicate-url-candidates-cw09-cw12-cc15: install
	src/main/bash/deduplicate.sh \
		--input sigir21/url-deduplication-cw09-cw12-cc15/part*near-duplicate-tasks/*.bz2 \
		--output sigir21/near-duplicates/url-simhash-one-grams-cw09-cw12-cc15/

repartition-url-candidates-cc17: install
	./src/main/bash/repartition-url-candidates.sh \
		--input s3a://corpus-copycat/document-representations/cc-2017-04/*.bz2 \
		--output sigir21/docs-in-cc17-repartitioned-by-url

create-source-docs: install
	./src/main/bash/run-low-resource-spark-job.sh de.webis.copycat_spark.spark.SparkCreateSourceDocuments

repartition-source-docs-cc15: install
	./src/main/bash/run-spark-job.sh de.webis.copycat_spark.spark.util.SparkRepartitionCommonCrawl2015SourceDocuments

repartition-source-docs-cc17: install
	./src/main/bash/run-spark-job.sh de.webis.copycat_spark.spark.util.SparkRepartitionCommonCrawl2017SourceDocuments

create-ids-to-include: install
	./src/main/bash/ids-to-include.sh \
		--allIds s3a://corpus-copycat/document-representations/cw09/ \
		--docRepresentations True \
		--exclusionIds sigir21/docs-to-remove/cw09 \
		--partitions 100 \
		--output sigir21/ids-to-include/cw09-bzip2 && \
	./src/main/bash/ids-to-include.sh \
		--allIds s3a://corpus-copycat/document-representations/cw12/ \
		--docRepresentations True \
		--exclusionIds sigir21/docs-to-remove/cw12 \
		--partitions 100 \
		--output sigir21/ids-to-include/cw12-bzip2 && \
	./src/main/bash/ids-to-include.sh \
		--allIds s3a://corpus-copycat/document-representations/cc-2015-11/ \
		--docRepresentations True \
		--exclusionIds sigir21/docs-to-remove/cc15 \
		--partitions 100 \
		--output sigir21/ids-to-include/cc15-bzip2 && \
	./src/main/bash/ids-to-include.sh \
		--allIds s3a://corpus-copycat/document-representations/cc-2017-04/ \
		--docRepresentations True \
		--exclusionIds sigir21/docs-to-remove/cc17 \
		--partitions 100 \
		--output sigir21/ids-to-include/cc17-bzip2 && \
	./src/main/bash/ids-to-include.sh \
		--allIds s3a://corpus-copycat/third-party/ids-in-clueweb09b \
		--exclusionIds s3a://corpus-copycat/documents-to-remove/cw09b-ids-to-remove-bzip2/ \
		--output sigir21/ids-to-include/cw09b-ids-to-include-bzip2 && \
	./src/main/bash/ids-to-include.sh \
		--allIds s3a://corpus-copycat/third-party/ids-in-clueweb12b13 \
		--exclusionIds s3a://corpus-copycat/documents-to-remove/cw12b13-ids-to-remove-bzip2/ \
		--output sigir21/ids-to-include/cw12b13-ids-to-include-bzip2

install:
	cd .. && \
	./mvnw clean install -f copycat-modules/interfaces/pom.xml 2> /dev/null && \
	./mvnw clean install -f copycat-modules/document-preprocessing/pom.xml 2> /dev/null && \
	./mvnw clean install -f copycat-modules/anserini-integration/pom.xml 2> /dev/null && \
	./mvnw clean install -f copycat-spark/pom.xml 2> /dev/null && \
	./mvnw clean install -f copycat-cli/pom.xml

label-data-maik:
	./src/main/bash/label-data-maik.sh

canonical-edges.pdf: src/main/python/plot.py
	python3 src/main/python/plot.py

jupyter-notebook:
	docker run -ti --rm -p 8888:8888 \
		-v ${PWD}:/workdir \
		-v /mnt/ceph/storage/data-in-progress/data-research/web-search/:/mnt/ceph/storage/data-in-progress/data-research/web-search/ \
		-w /workdir \
		capreolus:0.2.5 \
		jupyter notebook --no-browser --ip=0.0.0.0 --allow-root

