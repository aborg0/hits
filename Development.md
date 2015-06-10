# Prerequisities #

It is useful to have a target platform set before you start the development. That should contain a normal KNIME setup with distmatrix (from labs), jep installed.

Your IDE should have [FindBugs](http://findbugs.cs.umd.edu/eclipse), [TestNG](http://beust.com/eclipse), [Groovy](http://dist.codehaus.org/groovy/distributions/update/), Maven ([Maven Integration for Eclipse (m2e)](http://m2eclipse.sonatype.org/update/) or [IAM - Integration for Apache Maven (Q)](http://q4e.googlecode.com/)), [Buckminster plugins](http://www.eclipse.org/buckminster/downloads.html) (**important to select only the proper SVN plugins**), **one** of SVN team providers ([Subversive](http://www.polarion.com/products/svn/subversive.php?src=eclipseproject), or [Subclipse](http://subclipse.tigris.org/update_1.6.x)) installed.

You need to set the `JAVA_HOME` variable in your Windows machine pointing to a JDK root folder (which has `xjc.exe` tool in its `bin` folder.).

# Overview of projects #
  * [ie.tcd.imm.common](http://fisheye2.atlassian.com/browse/hits/ie.tcd.imm.hits.common/trunk/ie.tcd.imm.hits.common) - common utilities
  * [ie.tcd.imm.hits](http://fisheye2.atlassian.com/browse/hits/ie.tcd.imm.hits/trunk/ie.tcd.imm.hits) - the main nodes are here
  * (ie.tcd.imm.hits.3rdparty - 3rd party dependencies - no longer used)
    * Check this project for 3<sup>rd</sup> party dependencies: [ie.tcd.imm.hits.maven.parent](http://fisheye2.atlassian.com/browse/hits/trunk/ie.tcd.imm.hits.maven.parent)
  * [ie.tcd.imm.hits.interop](http://fisheye2.atlassian.com/browse/hits/ie.tcd.imm.hits.interop/trunk/ie.tcd.imm.hits.interop) - interoperability with other projects, like HCDC
  * [ie.tcd.imm.hits.exp](http://fisheye2.atlassian.com/browse/hits/ie.tcd.imm.hits.exp/trunk/ie.tcd.imm.hits.exp) - the nodes depending on KNIME labs nodes
  * [ie.tcd.imm.hits.feature](http://fisheye2.atlassian.com/browse/hits/trunk/ie.tcd.imm.hits.feature) - feature for the easier deploy
  * [ie.tcd.imm.knime.util](http://fisheye2.atlassian.com/browse/hits/ie.tcd.imm.knime.util/trunk/ie.tcd.imm.knime.util) - some utility nodes for KNIME
  * [ie.tcd.imm.knime.util.feature](http://fisheye2.atlassian.com/browse/hits/trunk/ie.tcd.imm.knime.util.feature) - feature for the utility nodes
  * [ie.tcd.imm.knime.test](http://fisheye2.atlassian.com/browse/hits/ie.tcd.imm.knime.test/trunk) - some tests
  * [ie.tcd.imm.hits.update](http://fisheye2.atlassian.com/browse/hits/trunk/ie.tcd.imm.hits.update) - the update site


# Materialisation #
Download one of the CQueries from [this folder](http://hits.googlecode.com/svn/trunk/general/). Resolve and materialize it. You should have a view of the necessary projects.