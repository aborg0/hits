# Custom scoring method version A.
scoreReplicatesBycustomA <- function (object)
{
    # The default implementation is the zscore for the each replicate group.
    xnorm <- Data(object)
    samps <- (wellAnno(object)=="sample")
    xnorm[] <- apply(xnorm, 2:3, function(v) (v-median(v[samps], na.rm=TRUE))/mad(v[samps], na.rm=TRUE))
    return(xnorm)
}

# Custom scoring method version B.
scoreReplicatesBycustomB <- function (object)
{
    # The default implementation is the zscore for each physical plate.  
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
                xnorm[idx, replicate, ch] <- ((xnorm[idx, replicate, ch]-median(xnorm[idx[samps], replicate, ch], na.rm=TRUE))/mad(xnorm[idx[samps], replicate, ch], na.rm=TRUE))
    }
    return(xnorm)
}

# Custom scoring method version C.
scoreReplicatesBycustomC <- function (object)
{
    # The default implementation is the zscore for each physical plate.  
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
                xnorm[idx, replicate, ch] <- ((xnorm[idx, replicate, ch]-median(xnorm[idx[samps], replicate, ch], na.rm=TRUE))/mad(xnorm[idx[samps], replicate, ch], na.rm=TRUE))
    }
    return(xnorm)
}