Import the workflow (compare_clustering.zip, or simple_dendrogram.zip),
and use the sample data from ../../plate8
(http://hits.googlecode.com/svn/ie.tcd.imm.knime.test/trunk/testdata/plate8)
for the INCell xls Importer node.
(For compare clustering: p01A.xls, p01B.xls, p01C.xls is recommended,
for simple dendrogram: p01A.xls, p02A.xls were used.)
You might consider changing the loop count in compare clustering.
If you select large number of repetition you get better distribution,
but in that case it is *strongly recommended* to change logging level to Error.