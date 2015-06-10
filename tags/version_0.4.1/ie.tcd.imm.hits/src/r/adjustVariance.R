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
    samps <- (wellAnno(object)=="sample")

    ## check if 'batch' slot is available:
    ## (as it is defined, the batch slot allows to have batches changing with plate,
    ## replicate and channel.
    ## 'batch' slot should be an array with the same dimensions of Data(object)
    bb <- batch(object)
    if(is.null(bb))
        stop("Please add the batch information to the slot 'batch'. This should be ",
             "an array with the same dimension of 'Data(object)' with numeric values 1, ",
             "2, etc. giving the batch number for each plate, sample and channel.")
    if(any(dim(bb)!=d))
        stop(sprintf("'batch' should have dimensions 'Features x Samples x Channels' (%s).",
                     paste(d, collapse=" x ")))

    bb <- batch(object)
    ## we assume that measurements from the same plate cannot belong to different batches.
    for(ch in 1:dim(bb)[3]) 
        for(r in 1:dim(bb)[2])
        {
            sp <- split(bb[,r,ch], plate(object))
            spp <- sapply(sp, function(j) length(unique(j)))
            if(any(spp>1))
                stop(paste("Measurements from plate", which(spp>1)[1] ,
                           "occur in multiple batches:", 
                           paste(unique(sp[[which(spp>1)[1]]]), collapse=", "),
                           "\ncurrently this program does not know how to handle this."))
        }
    nrBatches <- nbatch(object)
    for(r in 1:nrSamples)
    {
        for(ch in 1:nrChannels)
        {
            platesPerBatch <- split(plate(object), bb[,r,ch])
            nrB <- length(platesPerBatch) # this number depends on the channel and replicate
            for(b in 1:nrB)
            {
                pb <- platesPerBatch[[b]]
                plateInd <- (1+nrWpP*(min(pb)-1)):(nrWpP*max(pb))
                spp <- samps[plateInd]
                xnorm[plateInd,r,ch] <-
                    xnorm[plateInd,r,ch]/mad(xnorm[plateInd,r,ch][spp], na.rm=TRUE)
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
