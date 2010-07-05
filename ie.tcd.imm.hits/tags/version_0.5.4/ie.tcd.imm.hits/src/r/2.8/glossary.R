## glossary.R
## A glossary is a 3 colums data.frame 
## col[1] : words to be defined
## col[2] : simple definition of the word on his left : these definitions are visible in index.html when putting the mouse cursor over the word
## col[3] : a more complete definition of the word in column 1. These definitions will be writen in glossary.html with the saveHtmlGlossary function


## setDefinition enables to set a new definition into the glossary
## word : word to be defined
setDefinition <- function(glossary, word, simpleDefinition, completeDefinition)
{
    toAdd <- data.frame(word=word, simpleDefinition=simpleDefinition,
                        completeDefinition=completeDefinition)
    newGlossary <- rbind(glossary,toAdd)
    return(newGlossary)
}


## making of the glossary. Insertion of the definitions here 
createGlossary <- function()
{	
    glossary <- data.frame()
	
    ## Average dynamic range
    simpleDef <- "The average dynamic range is the mean of the replicate dynamic ranges (see replicate dynamic range) for the plate over all the replicates"
    completeDef <- "The average dynamic range is the mean of the replicate dynamic ranges (see below) for the plate over all the replicates"
    glossary <- setDefinition(glossary, "Average dynamic range", simpleDef, completeDef)  

    ## Replicate dynamic range
    simpleDef <- "The replicate dynamic range is an indicator of the gap between the positive and the negative controls"
    completeDef <- "The replicate dynamic range provides an indicator of the gap between the positive and the negative controls. <br/><br/>Depending of the value of the 'scale' attribute from the \"summarizePlate\" function, the replicate dynamic range is either : <br/>- the difference between the arithmetic average on positive and negative controls <br/>- the ratio between the geometric averages on positive and negative controls. <br/><br/>By default the choice is based on the scale of the data : if the scale is positive, then it is the ratio of geometric average, otherwise, it is the difference of arithmetic averages."
    glossary <- setDefinition(glossary, "Replicate dynamic range", simpleDef,completeDef)
	
    ## Repeatability standard deviation
    simpleDef <- "The repeatability standard deviation is the standard deviation for the random variable <i>standard deviation for a gene across all the replicates</i>. It is used to determine if the results for each replicate are similar or not. 0 is the perfect score, whereas a high value means the results are very different."
    completeDef <- "The repeatability standard deviation is the standard deviation for the random variable 'standard deviation for a gene across all the replicates'. It is used to determine if the results for each replicate are similar or not. <br/><br/>0 is the perfect score, whereas a high value means the results are very different."
    glossary <- setDefinition(glossary, "Repeatability standard deviation", simpleDef, completeDef) 
	
    ## "Spearman rank correlation "
    simpleDef <- "The Spearman rank correlation coefficient is a mesure of correlation between two replicates. A result close to zero means there is no correlation between the two replicates, whereas a result close to 1 means there is a strong correlation between them."
    completeDef <- "The Spearman rank correlation coefficient is a mesure of correlation between two replicates. <br/><br/>A result close to zero means there is no correlation between the two replicates, whereas a result close to 1 means there is a strong correlation between them."	
    glossary <- setDefinition(glossary, "Spearman rank correlation ",simpleDef,completeDef)
	
    ## "Spearman rank correlation (min - max)"
    simpleDef <- "This values correspond to the min and the max of the Spearman rank correlation among replicates. The Spearman rank correlation coefficient is a mesure of correlation between replicates. A result close to zero means there is no correlation between the two replicates, whereas a result close to 1 means there is a strong correlation between them."
    completeDef <- "This values correspond to the min and the max of the Spearman rank correlation among replicates"
    glossary <- setDefinition(glossary, "Spearman rank correlation (min - max)",simpleDef,completeDef)

    ## The Plate List module
    simpleDef <- "A table of quality scores for each plate, replicate and channel. This also serves as the navigation for drill-down to more detailed information for a given set of plates."
    completeDef <- ""
    glossary <- setDefinition(glossary, "Plate List", simpleDef, completeDef)

    ## The Plate Configuration module
    simpleDef <- "A graphical representation of the plate configurations. For each plate, the physical location of the positive and negative controls as well as the position of samples and possibly further types of probes are indicated by color coding."
    completeDef <- ""
    glossary <- setDefinition(glossary, "Plate Configuration", simpleDef, completeDef)

    ## The Plate Summaries module
    simpleDef <- "A set of diagnostic plots which provide an overview over the general screen quality. In particular, a boxplot of raw and, if applicable, normalized data values for the individual plates, and dot plots and density plots of the distribution of positive and negative controls, but only if the configure function has been called before."
    completeDef <- ""
    glossary <- setDefinition(glossary, "Plate Summaries", simpleDef, completeDef)

    simpleDef <- "A comprehensive plot of the the experiment results. The scored values for each plate are indicated by color coding on a rectangular grid."
    completeDef <- ""
    glossary <- setDefinition(glossary, "Screen Summary", simpleDef, completeDef)

     simpleDef <- "A table of the experiment result, including annotation information and a number of QC values. For data sets of reasonable size a condensed sortable table is rendered. Due to performance issues there will only be a link to a downloadable ASCII file for bigger data sets. The ASCII table contains additional information compared to the rendered version."
    completeDef <- ""
    glossary <- setDefinition(glossary, "Screen Results", simpleDef, completeDef)

     simpleDef <- "Background information about the experiment and about the normalization, summarization, and scoring method that has been applied."
    completeDef <- ""
    glossary <- setDefinition(glossary, "Screen Description", simpleDef, completeDef)

    simpleDef <- "The R script containing the code instruction to produce this report."
    completeDef <- ""
    glossary <- setDefinition(glossary, "Analysis Script", simpleDef, completeDef)

    simpleDef <- "Detailled quality report for a single plate across all replicates and channels."
    completeDef <- ""
    glossary <- setDefinition(glossary, "experimentQC", simpleDef, completeDef)

    simpleDef <- "Pdf version of the image."
    completeDef <- ""
    glossary <- setDefinition(glossary, "pdf", simpleDef, completeDef)

    simpleDef <- "Switch off the tooltips."
    completeDef <- ""
    glossary <- setDefinition(glossary, "switchHelp", simpleDef, completeDef)

    simpleDef <- "Download the complete screen result table as a tab-delimited ASCII file. You can import this file into any spreadsheet program of your choice."
    completeDef <- ""
    glossary <- setDefinition(glossary, "downloadTable", simpleDef, completeDef)

    simpleDef <- "The reproducibility of the measurement values between replicates. For the case of two replicates, this is a simple scatter plot. For more than two replicates we plot the correlation matrix. Please note that the assumption of correlated data does not hold for all experiment designs; e.g., most of the measurement values could be random noise, in which case they would be completely independent."
    completeDef <- ""
    glossary <- setDefinition(glossary, "Correlation", simpleDef, completeDef)

    simpleDef <- "Histograms of the raw or normalized measurement values for the respective replicates. This can be useful to compare data distributions between replicates."
    completeDef <- ""
    glossary <- setDefinition(glossary, "Histograms", simpleDef, completeDef)

    simpleDef <- "Histogram of the raw or normalized measurement values."
    completeDef <- ""
    glossary <- setDefinition(glossary, "Histogram", simpleDef, completeDef)

    simpleDef <- "A plate plot of standard deviations between replicates. Abnormal spatial pattern or very high values can indicate problems with one of the replicates."
    completeDef <- ""
    glossary <- setDefinition(glossary, "Reproducibility", simpleDef, completeDef)

    simpleDef <- "A plate plot of raw or normalized intensities. Plotting the data as arranges on the assay plate helps to identify spatial abnormalities."
    completeDef <- ""
    glossary <- setDefinition(glossary, "Intensities", simpleDef, completeDef)


    simpleDef <- "The correlation of the measurement values between two channels in a scatter plot. Depending on the experimental design, the two channels can be completely independent, and strong correlation could indicate problems."
    completeDef <- ""
    glossary <- setDefinition(glossary, "Channel Correlation", simpleDef, completeDef)
       
    return(glossary)
}


## getDefinition returns the (simple) definition associated to the word given in argument
getDefinition <- function(word, glossary)
{
    index <- match(word, glossary[,1])
    return(glossary[,2][index])	
}


## Saving the glossary as a html file
saveHtmlGlossary <- function(glossary, targetGlossary)
{  
    targetGlossaryFile=openPage(targetGlossary, title = "Glossary")
    hwrite('Glossary',targetGlossaryFile,heading=1,center=T,br=T)
    ## we only want the complete definitions in the html report
    htmlGlossary <- data.frame(word=glossary$word,def=glossary$completeDefinition)
    htmlGlossary <- htmlGlossary[htmlGlossary$def!="",]
    mcolor <- dataframeColor(htmlGlossary)  
    hwrite(htmlGlossary,targetGlossaryFile,col.names=FALSE,row.names=FALSE,bgcolor=mcolor,border=1,
           center = TRUE, col.width = c("25%", "75%")) 
    closePage(targetGlossaryFile)
}
