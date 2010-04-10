# All rights reserved. (C) Copyright 2009, Trinity College Dublin
# Copyright 2008 Michael Boutros and Ligia P. Bras L and Wolfgang Huber

## NB - since 05.11.2007, replicate scoring and summarization were split into 2 functions: "scoreReplicates"
## and "summarizeReplicates", in order to make the preprocessing work-flow clearer (these two steps were formely
## done sequentially by calling "summarizeReplicates").
## Now, summarizeReplicates **only** does what its name indicates: takes the chosen summary from the replicate
## data at each well.
## This function should be called **after** scoring the replicates through calling function "scoreReplicates".


## ======================================================================
## Replicates scoring
## ======================================================================

## Function that scores the replicate measurements given the specified method.
## Currently implemented scoring methods are:
##    none - don't do anything. Just multiply by "sign".
##   "zscore" - each replicate measurement is subtracted by the per-experiment median (at sample wells)
##              and then the result is divided by the per-experiment MAD (at sample wells).
##   "NPI" - normalized percent inhibition (applied in a per-replicate basis, i.e. using the overall mean
##           of positive and negative controls across all plates from a given replicate). For each replicate,
##           this method consists in subtracting each measurement from the average of the intensities on the
##           positive controls (considering all plate together), and this result is divided by the difference
##           between the averages of the measurements on the positive and the negative controls (overall plates).
##           In this case, we may need to provide further arguments (i.e., "posControls" and "negControls").
##
## added by Ligia Bras, 11.05.2007


# "..." - further arguments required by other scoring methods
# method = c("none", "zscore", "NPI")
scoreReplicates <- function(object, sign="+", method="zscore", ...)
{
    methodArgs <- list(...)
    ## 1) Score each replicate using the selected method:
    xnorm <- if(method=="none") Data(object) else do.call(paste("scoreReplicates", method, sep="By"),
                args=c(list(object), methodArgs))
    ## Store the scores in 'assayData' slot. 

    ## 2) Use "sign" to make the meaning of the replicates summarization 
    ## independent of the type of the assay
    sg <- switch(sign,
                 "+" = 1,
                 "-" = -1,
                 stop(sprintf("Invalid value '%s' for argument 'sign'", sign)))

    Data(object) <- sg*xnorm
    validObject(object)
    object@processingInfo[["scored"]] <- method
    return(object)
}



scoreReplicatesByzscore <- function(object)
{
    xnorm <- Data(object)
    samps <- (wellAnno(object)=="sample")
    xnorm[] <- apply(xnorm, 2:3, function(v) (v-median(v[samps], na.rm=TRUE))/mad(v[samps], na.rm=TRUE))
    return(xnorm)
}


scoreReplicatesByzscoreByPlate <- function(object){
	xnorm <- Data(object)
	objDim <- dim(xnorm)
	wellCount <- prod(pdim(object))
	plateCount <- objDim[1] / wellCount
	replicateCount <- objDim[2]
	channelCount <- objDim[3]
	
	for (plate in 1:plateCount){
		idx <- (1:wellCount)+wellCount*(plate - 1)
		samps <- (wellAnno(object)=="sample")
#		xnorm[idx, , ] <- apply(xnorm[idx, , ], 2:3, function(v) (v-median(v[idx[samps]], na.rm=TRUE)/mad(v[idx[samps]], na.rm=TRUE)))
		for (replicate in 1:replicateCount)
			for (ch in 1:channelCount)
				xnorm[idx, replicate, ch] <- ((xnorm[idx, replicate, ch]-median(xnorm[idx[samps], replicate, ch], na.rm=TRUE))/mad(xnorm[idx[samps], replicate, ch], na.rm=TRUE))
	}
	return(xnorm)
}


scoreReplicatesByzscoreNonRobust <- function(object){
	xnorm <- Data(object)
	samps <- (wellAnno(object)=="sample")
	xnorm[] <- apply(xnorm, 2:3, function(v) (v-mean(v[samps], na.rm=TRUE))/sd(v[samps], na.rm=TRUE))
	return(xnorm)
}

scoreReplicatesByzscoreNonRobustByPlate <- function(object){
	xnorm <- Data(object)
	objDim <- dim(xnorm)
	wellCount <- prod(pdim(object))
	plateCount <- objDim[1] / wellCount
	replicateCount <- objDim[2]
	channelCount <- objDim[3]
	
	for (plate in 1:plateCount){
		idx <- (1:wellCount)+wellCount*(plate - 1)
		samps <- (wellAnno(object)=="sample")
		for (replicate in 1:replicateCount)
			for (ch in 1:channelCount)
				xnorm[idx, replicate, ch] <- ((xnorm[idx, replicate, ch]-mean(xnorm[idx[samps], replicate, ch], na.rm=TRUE))/sd(xnorm[idx[samps], replicate, ch], na.rm=TRUE))
	}
	return(xnorm)
}



scoreReplicatesByNPI <- function(object, posControls, negControls)
{
    xnorm <- Data(object)
    d <- dim(xnorm)
    nrSamples <- d[2]
    nrChannels <- d[3]
    wAnno <- as.character(wellAnno(object))

    ## Check consistency for posControls and negControls (if provided)
    if(!missing(posControls))
    {
        checkControls(posControls, nrChannels, "posControls")
    }
    else
    {
        posControls <- as.vector(rep("^pos$", nrChannels))
    }

    if(!missing(negControls))
    {
        checkControls(y=negControls, len=nrChannels, name="negControls")
    }
    else
    {
        negControls=as.vector(rep("^neg$", nrChannels))
    }

    for(ch in 1:nrChannels)
    {
        if(!(emptyOrNA(posControls[ch]))) pos <- findControls(posControls[ch], wAnno) else pos <- integer(0)
        if(!(emptyOrNA(negControls[ch]))) neg <- findControls(negControls[ch], wAnno)  else neg <- integer(0)
        if (!length(pos) | !length(neg))
            stop(sprintf("No positive or/and negative controls were found in channel %d! Please use a ",
                         "different normalization function.", ch))

	for(r in 1:nrSamples) 
            if(!all(is.na(xnorm[, r, ch])) )
            {
                if(all(is.na(xnorm[pos,r,ch])) | all(is.na(xnorm[neg,r,ch])))
                    stop(sprintf(paste("No values for positive or/and negative controls were found in",
                                       "replicate %d, channel %d! Please use a different normalization function."),
                                 replicate, channel))

            xnorm[,r,ch] <- (mean(xnorm[pos,r,ch], na.rm=TRUE) - xnorm[,r,ch])/(mean(xnorm[pos,r,ch], na.rm=TRUE) -
                                                   mean(xnorm[neg, r, ch], na.rm=TRUE))
            }
    }
    
    return(xnorm)
}



## ======================================================================
## Replicates summarization
## ======================================================================
summarizeReplicates <- function(object, summary="min", method=singleColor)
{
    if(dim(Data(object))[3]!=1 && singleColor==method)
        stop("Currently this function is implemented only for single-color data.")
    else if (perChannel!=method && singleColor!=method)
	    stop("Only single-color or per-channel summarization is supported.")

    ## 2) Summarize between scored replicates:
    ## we need these wrappers because the behavior of max(x, na.rm=TRUE) if all
    ##   elements of x are NA is to return -Inf, which is not what we want.
    myMedian <- function(x)
    {
        x <- x[!is.na(x)]
        ifelse(length(x)>=1, median(x), as.numeric(NA))
    }
    myMax <- function(x)
    {
        x <- x[!is.na(x)]
        ifelse(length(x)>=1, max(x), as.numeric(NA))
    }
    myMin <- function(x)
    {
        x <- x[!is.na(x)]
        ifelse(length(x)>=1, min(x), as.numeric(NA))
    }

    myFurthestFromZero <- function(x)
    {
        x <- x[!is.na(x)]
        ifelse(length(x)>=1, x[abs(x)==max(abs(x))][1], as.numeric(NA))
    }

    myClosestToZero <- function(x)
    {
        x <- x[!is.na(x)]
        ifelse(length(x)>=1, x[abs(x)==min(abs(x))][1], as.numeric(NA))
    }

    ## Root mean square: square root of the mean squared value of the replicates
    myRMS <- function(x)
    {
        x <- x[!is.na(x)]
        ifelse(length(x)>=1, sqrt(sum(x^2)/length(x)), as.numeric(NA))
    }

    ## 2) Summarize between replicates:
    xnorm <- Data(object)
    
    if(dim(xnorm)[2]>1)
    { # we don't need to do anything in case we don't have replicates!
    
	    channelCount <- dim(xnorm)[3]
	    score <- matrix(nrow=dim(xnorm)[1], ncol=channelCount, dimnames=list(featureNames(object), 1:channelCount))
        for (i in 1:channelCount)
	    {
            xnorm <- Data(object)[,,i]       # NB - the function is only implemented for one-channel data
            score[, i] <- switch(summary,
                                 mean = rowMeans(xnorm, na.rm=TRUE),
                                 median = apply(xnorm, 1, myMedian),#rowMedians(xnorm, na.rm=TRUE),
                                 max  = apply(xnorm, 1, myMax),
                                 min  = apply(xnorm, 1, myMin),
                                 rms = apply(xnorm, 1, myRMS),
                                 closestToZero = apply(xnorm, 1, myClosestToZero),
                                 furthestFromZero = apply(xnorm, 1, myFurthestFromZero),
                                 stop(sprintf("Invalid value '%s' for argument 'summary'", summary)))
	    }
        ## Store the scores in 'assayData' slot. Since now there's a single sample (replicate) we
        ## need to construct a new cellHTS object.
        if (perChannel==method)
        {
            xnorm <- Data(object) # construct a cellHTS object just with the first sample

            channelCount <- dim(xnorm)[3]
            z <- object[,1]
            score <- matrix(nrow=dim(xnorm)[1], ncol=channelCount, dimnames=list(featureNames(object), 1:channelCount))
            for (i in 1:channelCount)
            {
                xnorm <- Data(object)[,,i, drop=FALSE]
                score[, i] <- switch(summary,
                                     mean = rowMeans(xnorm, na.rm=TRUE),
                                     median = apply(xnorm, 1, myMedian),#rowMedians(xnorm, na.rm=TRUE),
                                     max  = apply(xnorm, 1, myMax),
                                     min  = apply(xnorm, 1, myMin),
                                     rms = apply(xnorm, 1, myRMS),
                                     closestToZero = apply(xnorm, 1, myClosestToZero),
                                     furthestFromZero = apply(xnorm, 1, myFurthestFromZero),
                                     stop(sprintf("Invalid value '%s' for argument 'summary'", summary)))
            }
#            assayData(z) <- assayDataNew(storage.mode = c("lockedEnvironment", "environment", "list"), "score_ch1"=matrix(score[,1], dimnames=list(featureNames(object[,1]), 1)), "score_ch2"=matrix(score[,2], dimnames=list(featureNames(object[,2]), 1)), "score_ch3"=matrix(score[,3], dimnames=list(featureNames(object[,1]), 1)))
            e=new.env()
            for (ch in 1:channelCount)
            {
                e[[paste("score",ch, sep="_ch")]]=matrix(score[,ch], dimnames=list(featureNames(object[,1]), 1))
            }
            assayData(z) <- do.call(assayDataNew, as.list(e))
        }
        else
        {
	        z <- object[, 1, drop=FALSE] # construct a cellHTS object just with the first sample
            assayData(z) <- assayDataNew("score"=matrix(score, dimnames=list(featureNames(object), 1)))
        }
    }
    else #end if dim(xnorm)[2]==1
    {
        z <- object
        if (perChannel == method)
        {
            #print(dim(Data(object)))
            e=new.env()
            for (ch in 1:dim(Data(object))[3])
            {
                e[[paste("score",ch, sep="_ch")]]=matrix(Data(object)[,1,ch], dimnames=list(featureNames(object[,1]), 1))
            }
            assayData(z) <- do.call(assayDataNew, as.list(e))
        }
    }
    ## NB - the state "scored" of the cellHTS object is only changed to TRUE after data scoring
    ## and replicate summarization.
    z@state[["scored"]] <- TRUE
    z@processingInfo[["summarized"]] <-  summary
    validObject(z)
    return(z)
}



##------------------------------------------------
## Sigmoidal transformation of the z-score values
##------------------------------------------------
## Function that applies a sigmoidal transformation with parameters z0 and lambda to the z-score values
## stored in a cellHTS object. The obtained results are called 'calls'. The transformation is given by:
##	1/(1+exp(-(z-z0)*lambda))
## This maps the z-score values to the interval [0,1], and is intended to expand the scale of z-scores
## with intermediate values and shrink the ones showing extreme values, therefore making the difference
## between intermediate phenotypes larger.

## x - scored cellHTS object
## z0 - centre of the sigmoidal transformation
## lambda - parameter that controls the smoothness of the transition from low values 
## to higher values (the higher this value, more steeper is this transition). Should be > 0 (but usually
## it makes more sense to use a value >=1)

scores2calls <- function(x, z0, lambda)
{
    ## check whether 'x' contains scored values:
    if(!state(x)[["scored"]])
        stop(sprintf(paste("'x' should be a 'cellHTS' object containing scored data!\nPlease check",
                     "its preprocessing state: %s"), paste(names(state(x)), "=", state(x), collapse=", ")))

    if(dim(Data(x))[3]!=1)
        stop("Currently this function is implemented only for single-color data.")

    if(!all(is.numeric(lambda) & is.numeric(z0)))
        stop("'z0' and 'lambda' should be numeric values.")

    if(!all(length(lambda)==1L & length(z0)==1L))
        stop("'z0' and 'lambda' should be numeric values of length one.")

    if(lambda <=0)
        stop("'lambda' should be a positive value!")


    trsf <- function(z) 1/(1+exp(-(z-z0)*lambda))
    z <- trsf(Data(x))
    assayData(x) <- assayDataNew("call"=matrix(z, dimnames=list(featureNames(x), 1)))
    return(x)
}

perChannel = "per-channel"
singleColor = "single-color"
