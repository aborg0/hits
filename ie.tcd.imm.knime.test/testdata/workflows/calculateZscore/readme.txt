Important!
This is really experimental workflow, requires human interactions during computation.
Please do not use if you are not sure how to use.

Import the workflow (calculateZscore.txt),
and use the sample data from ../../plate8
(http://hits.googlecode.com/svn/ie.tcd.imm.knime.test/trunk/testdata/plate8)
for the INCell xls Importer node.

To execute the workflow you have to install the HCDC plugins too. (http://hcdc.ethz.ch, update site:
http://hcdc.ethz.ch/downloads/update-site-release)

The importance of this workflow that it allows you to normalise using POC (per physical plates),
then robust Z score (for each replicate's samples) the results without the cellHTS2 node.