# Introduction #

From [0.3.0](Changelog#Changes_from_0.2.0_to_0.3.0.md) version of HiTS it is possible to use custom normalisation and scoring. Although it is available only in `knime.expert.mode` (most probably this will remain there forever). This wiki page helps to modify the sample implementations.

# Details #

You should go to your eclipse installation and in the `ie.tcd.imm.hits_`_latest installed version_`/bin/r` folder you will find `customNormalisation.R` and `customScoring.R` files. These are containing `customA`, `customB`, `customC` functions. Feel free to modify them to your needs. Every execute of cellHTS2 node with the selected custom method will use that one. On error you can check in your Rserve window's error message (start from command line, and in that case you will be able to read it, or forward the errors to a file).