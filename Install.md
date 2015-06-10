# Installation instructions #

This page describes how to install/update the plugin to an existing KNIME installation.

You may also want to check the [dependencies](Dependencies.md) page

This video shows the installation of KNIME, HCDC and HiTS (without the R, Rserve, cellHTS2 installs):

<a href='http://www.youtube.com/watch?feature=player_embedded&v=lscXtomHFI8' target='_blank'><img src='http://img.youtube.com/vi/lscXtomHFI8/0.jpg' width='640' height=480 /></a>

## Prerequisites ##

You need an installed KNIME 2.1.x (or above) installed on your computer. You can find instructions on how to install it at [this page](http://www.knime.org/downloads/knime).
If you are using an internet connection with proxy, you may want to set the appropriate preferences at File/Preferences.../_General_/_Network Connections_ tab. Please note you have to check **Enable proxy authentication** if you have to give user name and password for your proxy connection.

You may want to look at this presentation if you are in trouble: [HiTS Start and Setup KNIME](http://docs.google.com/Presentation?id=dcr7k9x6_32fs9kfrc5)
<a href='Hidden comment:  wiki:gadget url="http://hosting.gmodules.com/ig/gadgets/file/103153836024844296842/googledocument.xml" height="600" width="800" border="0" up_webpagename="http://docs.google.com/Presentation?id=dcr7k9x6_32fs9kfrc5" '></a>

~~**Important:** You have to install the _KNIME Distant Matrix Feature_ from the [KNIME labs](http://labs.knime.org/) before installing the 0.2.0 feature.~~

You might want to add (somewhere after `-vmargs`) the following line to your knime.ini, which will allow you to use all available functionality:
`-Dknime.expert.mode=true`

You can install additional nodes from the introduction page, or from the _Help_ menu.

You should consider to increase the memory available to KNIME: edit knime.ini and change the lines starting with -Xmx and -XX:MaxPermSize=. The numbers following them are memory amount. The m means megabyte. The -XX:MaxPermSize= option influence the memory available to plugin code, while the -Xmx influence the available maximal memory (to both data and program code).

## Install ##

You have to add a new update site to your KNIME installation. This should be: `http://hits.googlecode.com/svn/trunk/ie.tcd.imm.hits.update/`
You can set it in the Help/Software Updates.../Find and Install.../_Search for new features to install_ **Next**, **New Remote Site...** and you can set any name you remember for the _Name:_ field, and the `http://hits.googlecode.com/svn/trunk/ie.tcd.imm.hits.update/` URL for the _URL:_ field.

After you have set the update site, you can select it and click on **Finish**. (Here you may be prompted to your proxy user and password.)
Recommended list of features (after the added labs update site (`http://labs.knime.org/update/2.1`)):
|**Name**|**Version**|
|:-------|:----------|
|HiTS 3rd party components feature|0.5.0.200912041736|
|HiTS main feature|0.5.4.201007052109|
|HiTS experimental features|0.4.1.200912041736|
|HiTS imaging feature|0.1.0.200912041736|
|HiTS interoperability feature|0.0.4.200912311558|
|KNIME utilities|0.2.2.200912041736|

Accept the license. After every plugins are installed you selected, you will be asked to
restart your KNIME. Please do that.

Installation is complete. You can check if you see a HiTS group in your Node Repository, and some preferences in File/Preferences.../KNIME/HiTS Preferences

## Update ##
This is similar to the install process.
The possible options are:
  1. update all nodes with compatible updates
  1. update just the HiTS nodes

For the first option go to Help/Software Updates.../Find and Install.../_Search for updates of the currently_ installed features, than click **Finish**. (Here you may be prompted to your proxy user and password.) If you have selected service updates, you might have to select Help/Software Updates.../Find and Install.../_Search for new features to install_.

For the second option go to Help/Software Updates.../Find and Install.../_Search for new features to install_ **Next** and select the _HiTS_ site. Click on **Finish**. (Here you may be prompted to your proxy user and password.) If there are updates you will have an option to select them. (If there are no updates you will get a message: _No updates to the currently installed feature(s) found, try again later._ And you do not have to do anything.) You have to accept the licenses. And install the features. When you are asked to restart your KNIME, please do that, by answering **Yes**.

# Troubleshooting #

## The HiTS nodes are not visible in Node Repository ##

This might be because the labs' Distance Matrix feature was not installed before the install of HiTS. You have to uninstall HiTS, install the Distance Matrix feature, then install HiTS. Sorry about the confusion.

## ~~The `Dendrogram with Heatmap` and `Sort by Cluster` nodes are not available~~ ##
~~You have to enable the experimental nodes by adding `-Dknime.expert.mode=true` line to your `knime.ini` file (somewhare after the `-vmargs` line)~~

## Network problems ##

If you have not set properly the proxy configuration in your KNIME installation, you will have an error like this when you try to update or install your plugins:

`Network connection problems encountered during search.`

In the **Details** there is a message like this:

`  Unable to access "http://hits.googlecode.com/svn/trunk/ie.tcd.imm.hits.update/".`

`    Error parsing site stream. [Premature end of file.]`

`    Premature end of file.`

`    Error parsing site stream. [Premature end of file.]`

`    Premature end of file.`

(If you do not have internet connection online, you will get the same message.)
In this case please set it up properly.

## Installed/updated but not working ##

~~You experience no problem installing the HiTS version 0.2.0 feature (using KNIME based on eclipse 3.3) from the update site, but the HiTS nodes are not available.~~

~~This happens when you install HiTS version 0.2.0 before the KNIME labs' [Distance Matrix Feature](http://labs.knime.org/distance-matrix). In this case you have to uninstall HiTS (Help/Software Updates/Manage Configuration/ find HiTS main feature, right click and select Uninstall, restart KNIME), install the Distance Matrix Feature, install HiTS.~~

## Permgen space ##
You see something like this in the log:
`Permgen space`
and the nodes does not work.

In this case you should increase the -XX:MaxPermSize= parameter in knime.ini.