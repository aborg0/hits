# Introduction #

The aim of this project to help the **Hi** gh **T** hroughput **S** creening process. The current subgoal is developing nodes to [KNIME](http://knime.org/) helping the data analysis of the results.

# Details #

At current stage the plugin is able to import data (the summary by wells data) from IN Cell Analyser 1000 Excel output, analyse it using [CellHTS2](http://www.dkfz.de/signaling/cellHTS/), and use [KNIME plugins](http://knime.org/) to further analyse the results of the output of the [CellHTS2 node](CellHTS2Node.md). Also created some nodes to visualise the data in a plate-based form heatmaps, denrogram with heatmap, or just a heatmap.

Currently only cellHTS2 2.6.x is supported (the new 2.8.x series is not yet).

# Alternatives/Other useful tools #

  * [High Content Data Chain](http://hcdc.ethz.ch/)

# First steps after [install](Install.md) #
An easy approach to start is downloading the content of [this folder](http://hits.googlecode.com/svn/ie.tcd.imm.knime.test/trunk/testdata/plate8/), and import [this workflow](http://hits.googlecode.com/svn/ie.tcd.imm.knime.test/trunk/testdata/workflows/sampleCellHTS2/sample1.zip). After that you [should follow the instructions](http://hits.googlecode.com/svn/ie.tcd.imm.knime.test/trunk/testdata/workflows/sampleCellHTS2/readme.txt) to get familiar with this data analysis tool.

# Acknowledgement #

This development was supported by a [Marie Curie Fellowship](http://cordis.europa.eu/improving/fellowships/home.htm).