##=============================================================
## adjust data variance
## Arguments:
## object - cellHTS object
## type - type of variance adjustment. 
##        Options are: 
##		 - "none" (default) - no variance adjustment
##               - "byPlate" (per-plate variance scaling)
##               - "byBatch" (per batch of plates variance scaling)
##               - "byExperiment" (per experiment variance scaling) 
## Variance adjustment is performed separately to each replicate and channel.


## wrapper function
adjustVariance <- function(object, method)
{ 
    xnorm <- do.call(paste("adjustVariance", method, sep=""), args=list(object))
    Data(object) <- xnorm
    return(object)
}



## adjust by the plate-wide mad
adjustVariancebyBatch <- function(object)
{
    ## use the array stored in slot 'assayData' (which in the workflow of 'normalizeChannels'
    ## function corresponds to already plate corrected data).
    xnorm <- Data(object) 
    d <- dim(xnorm)
    nrWpP <- prod(pdim(object))
    nrSamples <- d[2]
    nrChannels <- d[3]
    nrPlates <- max(plate(object))
    samps <- (wellAnno(object)=="sample")

    ## check if 'batch' info is available:
    bb <- batch(object)
    if(is.null(bb))
        stop("Please add the batch information using the 'batch' method. This should be ",
             "an array with number of rows equal to number of plates and number of columns",
             " equal to number of samples.")
    if(nrow(bb) != nrPlates || ncol(bb) != nrSamples)
        stop(sprintf("'batch' should have dimensions 'Plates x Samples' (%s).",
                     paste(c(nrPlates, nrSamples), collapse=" x ")))
    nrBatches <- nbatch(object)
    for(r in 1:nrSamples)
    {
        #wellsPerBatch <- split(plate(object), bb[,r,ch])
        for(ch in 1:nrChannels)
        {
            #platesPerBatch <- split(plate(object), bb[,r,ch])
            #nrB <- length(platesPerBatch) # this number depends on the channel and replicate
            for(b in 1:nrBatches)
            {
                thisBatch <- which(bb[,r] == b)
                if(length(thisBatch))
                {
                    plateInd <- as.vector(mapply(function(from, to) from:to, to=thisBatch*nrWpP,
                                                 from=(thisBatch*nrWpP)-nrWpP+1))
                    spp <- samps[plateInd]
                    xnorm[plateInd,r,ch] <-
                        xnorm[plateInd,r,ch]/mad(xnorm[plateInd,r,ch][spp], na.rm=TRUE)
                }
            }#batch
        }#channel
    }#sample
    return(xnorm)
}




adjustVariancebyExperiment <- function(object)
{
    ## use the array stored in slot 'assayData' (which in the workflow of
    ## 'normalizeChannels' function corresponds to already plate corrected data).
    xnorm <- Data(object) 
    samps <- (wellAnno(object)=="sample")

    ## adjust by the experiment-wide mad
    xnorm[] <- apply(xnorm, 2:3, function(z) z/mad(z[samps], na.rm=TRUE)) 
    return(xnorm)
} 



adjustVariancebyPlate <- function(object)
{
    ## use the array stored in slot 'assayData' (which in the workflow of
    ## 'normalizeChannels' function corresponds to already plate corrected data).
    xnorm <- Data(object) 
    d <- dim(xnorm)
    nrWpP <- prod(pdim(object))
    nrPlates <- max(plate(object))
    samps <- (wellAnno(object)=="sample")

    ## adjust by the plate-wide mad
    for (p in 1:nrPlates)
    {
        plateInd <- (1:nrWpP)+nrWpP*(p-1)
        spp <- samps[plateInd]
        xnorm[plateInd,,] <-
            apply(xnorm[plateInd,,,drop=FALSE], 2:3, function(z) z/mad(z[spp], na.rm=TRUE))
    }
    return(xnorm)
}
