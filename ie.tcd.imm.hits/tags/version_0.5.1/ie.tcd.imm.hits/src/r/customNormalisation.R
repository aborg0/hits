# Custom normalisation method, version A
customA <- function(object, scale, posControls, negControls, ...)
{
    # This implementation is the x*median_{plate}(x_neg) / median_{plate}(x_pos) 
    # See this thread for details: http://www.knime.org/node/555
    xnorm <- Data(object)
    
    d <- dim(xnorm)
    nrWpP <- prod(pdim(object))
    nrPlates <- max(plate(object))
    nrSamples <- d[2]
    nrChannels <- d[3]
    
    wellAnnotation <- as.character(wellAnno(object))
    for(p in 1:nrPlates)
    {
        plateInds <- (1:nrWpP)+nrWpP*(p-1)
        wAnno <- wellAnnotation[plateInds]
        
        for(ch in 1:nrChannels)
        {
            if(!(emptyOrNA(posControls[ch]))) pos <- findControls(posControls[ch], wAnno) else pos <- integer(0)
            if(!(emptyOrNA(negControls[ch]))) neg <- findControls(negControls[ch], wAnno)  else neg <- integer(0)
            
            for(r in 1:nrSamples)
                if(!all(is.na(xnorm[plateInds, r, ch])))
                    xnorm[plateInds, r, ch] <- xnorm[plateInds, r, ch] * (median(xnorm[neg, r, ch], na.rm=TRUE) / median(xnorm[pos, r, ch], na.rm=TRUE))
        }
    }
    Data(object) <- xnorm
    return(object)
}

# Custom normalisation method, version B
customB <- function(object, scale, posControls, negControls, ...)
{
    # Default implementation does no change
    return(object)
}

# Custom normalisation method, version C
customC <- function(object, scale, posControls, negControls, ...)
{
    # Default implementation does no change
    return(object)
}
