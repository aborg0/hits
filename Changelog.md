# HiTS #
## Changes from 0.5.4 to 0.5.5 ##
Date: 6th June 2012

Added support for cellHTS2 2.12 and 2.14 versions. This also introduced the loss of loess normalisation.

Bug fixes:
  * Problem with 96 well plates fixed.

## Changes from 0.5.3 to 0.5.4 ##
Date: 5th July 2010

Bug fixes:
  * Problem with html report fixed.
  * Incomplete plates are supported.

## Changes from 0.5.2 to 0.5.3 ##
Date: 10th April 2010

Bug fixes:
  * Plates with no samples should work now.

## Changes from 0.5.1 to 0.5.2 ##
Date: 1st March 2010

Bug fixes:
  * Spaces related bugs in the reader nodes.
  * Updating cellHTS2 to 2.10.5

## Changes from 0.5.0 to 0.5.1 ##
Date: 13th December 2009

Bug fixes:
  * INCell xls Importer
    * proper handling of file names with spaces in their path

## Changes from 0.4.1 to 0.5.0 ##
Date: 5th December 2009

Bug fixes:
  * biomaRt node
    * Now it is possible to use it behind firewalls
CellHTS2
  * support for cellHTS2 2.10.x (based on cellHTS2 2.10.2)
  * [Issue 27](https://code.google.com/p/hits/issues/detail?id=27) done.

## Changes from 0.4.0 to 0.4.1 ##
Date: 13th October 2009

Bug fixes:
  * Plate Heatmap
    * HiLite was working wrong on 96 well plates
    * Data preview on tooltips was not working on 96 well plates
    * non-continuous (also not starting from 1) plate/replicate values are now supported
  * CellHTS2
    * fix analysing single plates
    * allow usage of cellHTS2 2.4.x too

## Changes from 0.3.0 to 0.4.0 ##
Date: 20th August 2009

One new node:
> - **Scores to p-values** allows to compute P(Z<=x) (the Erf function) with an estamated frequency from a population.

The content is now more modular, separated the nodes depending on KNIME Lab's distmatrix to ie.tcd.imm.hits.exp, and introduced a lightweight ancestor project: ie.tcd.imm.hits.common.

Bugs fixed:
  * [Issue 11](https://code.google.com/p/hits/issues/detail?id=11) - configuration is now available in cellHTS2's outport
  * [Issue 22](https://code.google.com/p/hits/issues/detail?id=22) - variance adjustment works again
  * [Issue 23](https://code.google.com/p/hits/issues/detail?id=23) - contents no longer mixed in results
  * [Issue 25](https://code.google.com/p/hits/issues/detail?id=25) - more detailed error messages

New features:
  * The export of colour legend is now working better, more options present.
  * It is possible to suppress the generation of HTML report in cellHTS2.
  * Support for 384 well plates in Plate Heatmap, and the other nodes.
  * Small bug fixes.

New nodes in exp (0.4.0) plugin:
  * Leaf Ordering - computes the [optimal leaf ordering](http://bioinformatics.oxfordjournals.org/cgi/content/abstract/19/9/1070) for a hierarchical clustered tree.
  * Reverse Order - changes the ordering of the tree leaves to the opposite.

## Changes from 0.2.0 to 0.3.0 ##
Date: 29th May 2009

No new nodes, no incompatible updates.

Updated the the 3rd party dependencies project, to prevent any problems with possibly old Rserve. The bundled Rserve client is now 0.6.0.

  * [Issue 13](https://code.google.com/p/hits/issues/detail?id=13) fixed, the update site should work correctly with eclipse 3.4, or above.
  * [Issue 15](https://code.google.com/p/hits/issues/detail?id=15) done, added new example workflows, check [Usage](Usage#Try_it.md) for details.
  * [Issue 18](https://code.google.com/p/hits/issues/detail?id=18) fixed, this release is compatible with both cellHTS2 2.6.x, and cellHTS2 2.8.x. (Some problems identified in cellHTS2 2.8.0 also fixed.)
  * [Issue 19](https://code.google.com/p/hits/issues/detail?id=19) fixed, before overwrites now it asks.
  * [Issue 20](https://code.google.com/p/hits/issues/detail?id=20) fixed, the Distance Calculator Feature is now optional.

New features:
  * There is a tooltip in the well layout legend, indicating what is at that area.
  * It is possible to use custom normalisation/scoring methods when `knime.expert.mode` is `true` (put `-Dknime.expert.mode=true` after `-vmargs` in your knime.ini file to access this feature).
  * It is possible to save the colour legend in the heatmap nodes.

## Changes from 0.1.1 to 0.2.0 ##
Date: 5th May 2009 (available since 30th April 2009)

Added 3 new nodes:
  * Simple Heatmap (Data Views/)
  * Dendrogram with Heatmap (HiTS/) (based on the original Hierarchical Clustering node, thanks for the KNIME GMbH, but compatible only with the New Hierarchical Clustering node from KNIME labs)
  * Sort by Cluster (Distance Matrix/)

Renames:
  * Heatmap -> Plate Heatmap
  * Importer -> INCell xls Importer

Improved the Colour selection forms (except the one in the selection of default).

Documentation reviewed, improved. Many thanks to Dr. Dara Dunican.

Updated the update site, so from this point it will handle installs from eclipse 3.4 or above. See [issue #13](https://code.google.com/p/hits/issues/detail?id=#13).

Added an [example workflow](http://hits.googlecode.com/svn/ie.tcd.imm.knime.test/trunk/testdata/plate8/) and [sample data](http://hits.googlecode.com/svn/ie.tcd.imm.knime.test/trunk/testdata/workflows/sampleCellHTS2/). See [issue #15](https://code.google.com/p/hits/issues/detail?id=#15). Thanks for Amanda Birmingham to allow to use her NoiseMaker program to generate the data set.

More robust handling of input data with CellHTS2 node (support for missing values, NaNs, works correctly with 1 replicate, and more than 9 (but less than 1000) parameters).

The Plate Heatmap (also Simple Heatmap, Dendrogram with Heatmap) now accepts missing values too.

The nodes with views handle better their resets.

Added a Help button to the configuration of the CellHTS2 node. It opens the help page describing the options.

As a problem fixed in KNIME 2.0.2 a filter added to the parameter columns in CellHTS2 node. By default it excludes the Plate, Replicate, Position columns.

The Ranking node's configuration panel is fixed, not it shows the proper values for the first open too.

## Changes from 0.1.0 to 0.1.1 ##
Date: 4th March 2009

Some bugfixes, little improvements, an incompatible update (using normalise instead of normalize in [Rank](RankNode.md) node), added user documentation and some help to interpret the settings ([CellHTS2](CellHTS2Node.md) and [Heatmap](HeatmapNode.md) nodes).

# KNIME utilities #
## Changes from 0.2.1 to 0.2.2 ##
Date: 5th December 2009

Improved documentation: [Issue 26](https://code.google.com/p/hits/issues/detail?id=26)

## Changes from 0.2.0 to 0.2.1 ##
Date: 13th October 2009

Fix for the annoying behaviour when the result column name would be empty.

## Changes from 0.1.0 to 0.2.0 ##
Date: 20th August 2009

Created icons for those that did not have.

Added a new node:
> - Subsets - generates subsets of the selected columns.

## Changes from 0.0.1 to 0.1.0 ##
Date: 12th June 2009

The new version supports HiLite in these nodes. Updated documentation.

Added a new node:
> - Merge - allows to reorder the rows for Pivot node.

## Version 0.0.1 ##
Available since: 15th April 2009 (as version 0.0.1)

3 nodes:
  * Direct product
  * Pivot
  * Unpivot

# Interoperability #
## Changes from 0.0.3 to 0.0.4 ##
Date: 5th December 2009

No real change, only dependency updates.

## Changes from 0.0.1 to 0.0.3 ##
Date: 13th October 2009

Not too much, just following the internal refactors and added a help button the the configuration dialog.

## Version 0.0.1 ##
Date: 20th August 2009

2 nodes:
  * BioConverter - converts between different column formats
  * Plate Format - converts between different plate layouts.

# Image handling #
## Changes from 0.0.3 to 0.1.0 ##
Date: 5th December 2009

Improved user interface (histogram, better colour adjustment options) of the image viewer, better support for Z-stacks, time-dependent series. Updated Bio-Formats to the recently released 4.1.1 version.
New node: LOCI to KNIME images - this node gives you the many possibilities of KNIME image processing for the images loaded with Bio-Formats.

## Changes from 0.0.1 to 0.0.3 ##
Date: 13th October 2009

Added (generated) help, the dependencies are now not embedded.
Bug fix:
  * [Issue 29](https://code.google.com/p/hits/issues/detail?id=29) fixed (Now every feature should be available from eclipse 3.5 based KNIME installations.)

## Version 0.0.1 ##
Date: 29th September 2009

2 nodes:
  * LOCI Plate Reader - reads images from plates using Bio-Formats.
  * LOCI Plate Viewer - shows images from plates using Bio-Formats.