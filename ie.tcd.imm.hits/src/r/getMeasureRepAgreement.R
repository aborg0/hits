## ---------------------------------------------------------------------------
## LPB, August 2007
## Function to calculate a measure of agreement between plate replicates.
## It calculates the repeatability standard deviation between replicate plates and the
## correlation between replicates.
## NOTE: these measures are calculated only for sample wells!
## 
## 	Arguments:
## 		x - cellHTS object
##               corr.method - correlation method to use: "pearson" or "spearman"
##	Output:
## 		out - list with elements: "repStDev" - matrix with the calculated repeatability
##                                         standard deviation between plate
##                                                        replicates (dim: nrPlates x nrChannels)
## 				            "corrCoef" - matrix with the correlation coefficients
##                                          between plate replicates 
##						         (dim: nrPlates x nrChannels) -
##                                            if nrReplicates==2
## 
## 				            "corrCoef.min" - (if nrReplicates >2) matrix with
##                                          the minimum correlation coefficients
## 							  between plate replicates
##                                                 (dim: nrPlates x nrChannels)
## 				            "corrCoef.max" - (if nrReplicates >2) matrix with
##                                                  the maximum correlation coefficients
##						     between plate replicates
##                                                   (dim: nrPlates x nrChannels)
## ---------------------------------------------------------------------------

getMeasureRepAgreement <- function(x, corr.method="spearman")
{
    ## consistency checks:
    if(!inherits(x, "cellHTS"))
        stop("'x' must be a 'cellHTS' object")

    ## Check the status of the 'cellHTS' object
    if(!state(x)[["configured"]])
        stop("Please configure 'x' (using the function 'configure') before calculating",
             "the agreement between plate replicates!")

    y <- Data(x)
    
    ## dimensions
    d <- dim(y)
    nrWells    <- prod(pdim(x))
    nrPlates   <- max(plate(x))
    nrReplicates <- d[2]
    nrChannels <- d[3]
    plates <- plate(x)
    
    repSdev <- corrCoef <- corrCoef.min <- corrCoef.max <-
        matrix(NA, nrow=nrPlates, ncol=nrChannels) 

    samps <- (as.character(wellAnno(x))=="sample")

    if(nrReplicates>1)
    { 
        for(ch in 1:nrChannels)
        {
            for(p in 1:nrPlates)
            {
                indp <- nrWells*(p-1)+c(1:nrWells)
                yy <- y[indp,,ch][samps[indp],] #y[samples[indp],p,,ch]
                rsums <- rowSums(!is.na(yy))
                keep <- (rsums >1)
                yy <- yy[keep,]
                mr <- apply(yy, 1, mean, na.rm=TRUE) #mean over all replicates for each gene j
                sr <- apply(yy,1, sd, na.rm=TRUE) #std for each gene j across all of the replicates
                ngenes <- sum(keep)
                if(ngenes) repSdev[p,ch] <- sqrt(sum(sr^2)/ngenes)

                yy <- y[indp,,ch][samps[indp],]
                repHasVals <- rowSums(t(!is.na(yy)))!=0
                if(sum(repHasVals)>1)
                {
                    yy <- yy[,repHasVals]
                    cmbs <- combn(1:ncol(yy), 2) 
                    zcor <- c()
                    for(j in 1:ncol(cmbs)) {
                        z <- yy[,cmbs[,j]]
                        zcor <- c(zcor, cor(z, method=corr.method, use="complete.obs")[1,2])
                    }
                    
                    if (length(zcor)>1)
                    { 
                        corrCoef.min[p,ch] <- min(zcor, na.rm=TRUE)
                        corrCoef.max[p,ch] <- max(zcor, na.rm=TRUE)
                    }
                    else
                    {
                        corrCoef.min[p,ch] <- zcor
                        corrCoef.max[p,ch] <- zcor
                        corrCoef[p,ch] <- zcor
                    }

                } # sum(repHasVals)
            } # plates
        }# channels
    } # replicates

    out <- if(nrReplicates==2) list(repStDev=repSdev, corrCoef=corrCoef) else
    list(repStDev=repSdev, corrCoef.min=corrCoef.min, corrCoef.max=corrCoef.max)
 
    return(out)
}
#===================================================================














