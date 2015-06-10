## ----------------------------------------------------------------------------
## Functions for data normalization and summarization
## ----------------------------------------------------------------------------

## All of these functions use the content of "assayData" slot of the NChannelSet-derived cellHTS object

## =============================================================================
## 	 	------- Per-plate normalization of raw data ---------
## modified by Ligia Bras, AUG 2007
## modified by Ligia Bras, NOV 2007
## 
## object - cellHTS instance
## scale - argument to define whether the data are in "additive" or "multiplicative" scale
## log - log transform the data? TRUE or FALSE . Note: cannot be TRUE if scale="additive".
## method - define the plate adjustment: "median", "mean", "shorth" (subtract or divide, depending on "scale" argument)
##                                       "POC", "negatives", "NPI"
##                                       "Bscore" (but without variance adjustment of the residuals. This can be done in a subsequent step)
## varianceAdjust - argument to specify which variance adjustment to perform. Options are: "byExperiment", ""byBatch" or "byPlate". 
## posControls- optional. Required if a controls-based normalization method is chosen. Defaults to "pos".
## negControls - optional. Required if a controls-based normalization method is chosen. Defaults to "neg"
##
## Function workflow: 
##   1. Log transformation (optional)
##   2. Plate adjustment using the chosen method
##   3. Variance adjustment (optional)
## =============================================================================

normalizePlates <- function(object, scale="additive", log = FALSE, method="median", varianceAdjust="none",
                            posControls, negControls,...)
{

    if(!is(object, "cellHTS"))
        stop("'object' should be of class 'cellHTS'.")
    ## Check the status of the 'cellHTS' object
    if(!state(object)[["configured"]])
        stop("Please configure 'object' (using the function 'configure') before normalization.")

    ## Check the conformity between the scale of the data and the chosen preprocessing
    if(scale=="additive" & log)
        stop("For data on the 'additive' scale, please do not set 'log=TRUE'. ",
             "Please have a look at the documentation of the 'scale' and 'log' options ",
             "of the 'normalizePlates' function.") 

    if(!(varianceAdjust %in% c("none", "byPlate", "byBatch", "byExperiment"))) 
        stop(sprintf("Undefined value %s for 'varianceAdjust'.", varianceAdjust))

    ## Check consistency for posControls and negControls (if provided)
    nrChannel <- length(ls(assayData(object)))

    if(!missing(posControls))
    {
        checkControls(posControls, nrChannel, "posControls")
    }
    else
    { 
        posControls <- as.vector(rep("^pos$", nrChannel))
    }

    if(!missing(negControls))
    {
        checkControls(y=negControls, len=nrChannel, name="negControls")
    }
    else
    {
        negControls <- as.vector(rep("^neg$", nrChannel))
    }

    ## 1. Log transformation: 
    oldRawData <- Data(object)
    if(log)
    {
        Data(object) <- suppressWarnings(log2(oldRawData))
        if(any(oldRawData[!is.na(oldRawData)]==0))
            warning("Data contains 0 values.\n",
                    "Log transformation for those values resulted in -Inf",
                    call.=FALSE)
        if(min(oldRawData, na.rm=TRUE)<0)
            warning("Data contains negative values.\n",
                    "Log transformation for those values resulted in NA",
                    call.=FALSE) 
        scale <- "additive"
    }
    
    ## 2. Plate-by-plate adjustment:
    allowedFunctions <- c("mean", "median", "shorth", "negatives", "POC", "NPI", "Bscore", "loess", "locfit")
    ## overwrite assayData with the new data 
    object <- switch(method,
                     "mean" = perPlateScaling(object, scale, method),
                     "median" = perPlateScaling(object, scale, method),
                     "shorth" = perPlateScaling(object, scale, method),
                     "negatives" = perPlateScaling(object, scale, method, negControls),
                     "POC" = controlsBasedNormalization(object, method, posControls, negControls),
                     "NPI" = controlsBasedNormalization(object, method, posControls, negControls),
                     "Bscore" = Bscore(object, ...),
                     "loess" = spatialNormalization(object, model="loess", ...), 
                     "locfit" = spatialNormalization(object, model="locfit", ...),
                     "customA" = customA(object=object, scale=scale, posControls=posControls, negControls=negControls, ...),
                     "customB" = customB(object=object, scale=scale, posControls=posControls, negControls=negControls, ...),
                     "customC" = customC(object=object, scale=scale, posControls=posControls, negControls=negControls, ...),
                     stop(sprintf("Invalid value '%s' for argument 'method'.\n Allowed values are: %s.", 
                                  method, paste(allowedFunctions, collapse=", ")))
                     )

    ## 3. Variance adjustment (optional):
    if(varianceAdjust!="none")
        object <- adjustVariance(object, method=varianceAdjust)

    object@state[["normalized"]] <- TRUE
    if (regexpr("2\\.[46]\\..", package.version("cellHTS2")) != 1)
        object@processingInfo[["normalized"]] <- method
    validObject(object)
    return(object)
}
 

## =============================================================================
## 		-------- Channel summarization  -------
## Function that combines plate-corrected intensities from multi-channel screens.
## Modified by LPB, AUG 2007
## Modified by LPB, NOV 2007 - plate normalization before 
## =============================================================================
summarizeChannels <- function(object, fun=function(r1, r2, thresh=-Inf)
                              ifelse(r1>thresh, r2/r1, as.numeric(NA)))
{
    if(length(channelNames(object)) == 1)
        stop("This function is implemented only for multi-channel data.")
    
    xnorm <- Data(object) 
    ## The argument 'fun' allows using different normalizations, and also to define
    ## the numerator/denominator for the ratio (i.e. R1/R2 or R2/R1)
    nrChans <- dim(xnorm)[3]
    alist <- vector(mode="list", length=nrChans)
    names(alist) <- paste("r", seq_along(alist), sep="")
    for(i in seq_len(nrChans))
    {
        alist[[i]] <- xnorm[,,i]
    }
    xnorm <- array(do.call(fun, alist),
                   dim=c(dim(xnorm)[1:2], "Channels"=1))
    
    ## store the summarized data in 'assayData' slot:
    ## 1) remove channel 2:
    chNames <- assayDataElementNames(object) 
    assayDataElement(object, chNames[2:nrChans]) <- NULL
    ## 2) replace the contents of the (single) remaining channel by the new summarized values:
    Data(object) <- xnorm
    ## 3) State is now considered to be normalized
    if(!state(object)["normalized"])     
        object@processingInfo[["normalized"]] <- "channel summarization"
    object@state[["normalized"]] <- TRUE
    
    
    validObject(object)
    return(object)
}
