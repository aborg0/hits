Import the workflow (compare_clustering.zip, compare_clustering_with_alternatives.zip,
or simple_dendrogram.zip), and use the sample data from ../../plate8
(http://hits.googlecode.com/svn/ie.tcd.imm.knime.test/trunk/testdata/plate8)
for the INCell xls Importer node.
(For compare clustering: p01A.xls, p01B.xls, p01C.xls is recommended,
for simple dendrogram: p01A.xls, p02A.xls were used.)
You might consider changing the loop count in compare clustering.
If you select large number of repetition you get better distribution,
but in that case it is *strongly recommended* to change logging level to Error.

It is important to mention, that the counting the perfect triples is generalisable to count
the perfect doubles, quadruples, ... in that case you have to replace 1 with the standard deviation
(sample) of 1, ..., n.

The compare_clustering_with_alternatives.zip is comparing the clustering with different
selection of parameters. (If you want to decrease the list of the parameters you have to modify:
 - the keep parameters Column Filter
 - the Missing Value node inside Alternatives
 - the Distance Matrix Calculate in Alternatives/Generate alternatives/rank measure/Distance Matrix Calculate.)

The approach presented in the compare clustering projects is usable to compare the clusters for example:
 - by replicates (presented in projects)
 - by different concentrations
 - by different time points