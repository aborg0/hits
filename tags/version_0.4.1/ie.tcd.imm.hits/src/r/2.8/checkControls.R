# Auxiliary functions that perform common tests for the controls
checkControls <- function(y, len ,name="negControls")
{
    if(!is(y, "vector") | length(y)!=len | mode(y)!="character") 
        stop(sprintf("'%s' should be a vector of regular expressions with length %d",
                     name, len))
}



## function to check consistency of positive controls for a two-way assay:
checkControls2W <- function(y, len, name="posControls")
{
    if (length(y)!=2 || !identical(sort(names(y)), c("act", "inh")) ||
        any(sapply(y, length)!=len) ||
        any(sapply(y, mode)!="character"))#* 
        stop(cat(sprintf("'%s' should be a list with 
             two components: 'act' and 'inh'.\n These components 
             should be vectors of regular expressions with length %d \n", name, len)))
}



## Function to check if a character is empty
emptyOrNA <- function(y) y %in% c(NA, "")




## Function to find the indexes of a given control (identified by the regular expression y)
## in the well annotation character vector.
findControls <- function(y, anno)
{
    if(length(y)==1)
    {
        which(regexpr(y, anno, perl=TRUE)>0)
    }
    else
    {
        sapply(y, function(i) which(regexpr(i, anno, perl=TRUE)>0))
    }
}



## Auxiliary function that:
## 1) Checks controls annotation
## 2) Determines if the assay is one-way or two-way 
## 3) If the assay is one-way, determines the name of the positive controls.
## note plateConfContent corresponds to plateConf(x)$Content where x is a cellHTS object
checkPosControls <- function(posControls, nrChan, wellAnnotation, plateConfContent)
{
    twoWay <- FALSE
    namePos <- NULL
    if(!is(posControls, "list"))
    {
        checkControls(posControls, nrChan, "posControls")
        ## see if there are different positive controls (e.g. with different strengths)
        aux <- unique(posControls)
        aux <- findControls(aux[!emptyOrNA(aux)], wellAnnotation)
        if(length(aux))
        {
            namePos <- unique(unlist(sapply(aux, function(i) unique(wellAnnotation[i]))))
            namePos <- sort(plateConfContent[match(namePos, tolower(plateConfContent))]) 
        }
    }
    else
    {
        checkControls2W(posControls, len=nrChan, name="posControls")
        twoWay <- TRUE
    }
    return(list(namePos=namePos, twoWay=twoWay)) 
}



## Function  to get the indeces for the different controls
getControlsPositions <- function(posControls, negControls, isTwoWay, namePos,
                                 nrChannels, wAnno)
{
    actCtrls <- inhCtrls <- posCtrls <- negCtrls <- vector("list", length=nrChannels)
    if(isTwoWay)
    {
        aux <- c(1:nrChannels)[!emptyOrNA(posControls$act)]
        ## needs to be like this because of the case of length(aux)=1
        if(any(aux))
            actCtrls[aux] <- lapply(aux, function(i)
                                    as.numeric(findControls(posControls$act[i], wAnno))) 
        aux <- c(1:nrChannels)[!emptyOrNA(posControls$inh)]
        if(any(aux))
            inhCtrls[aux] <- lapply(aux, function(i)
                                    as.numeric(findControls(posControls$inh[i], wAnno)))
    }
    else
    {  ## oneWay
        posCtrls <- lapply(posCtrls, function(z){ 
                           z <- vector("list", length=length(namePos)) 
                           names(z) = namePos
                           return(z)
                       })
        aux <- c(1:nrChannels)[!emptyOrNA(posControls)]
        if(any(aux))
            posCtrls[aux] <- lapply(aux, function(i){
                wa <- findControls(posControls[i], wAnno)
                if(length(wa))
                {
                    wa <- split(wa, wAnno[wa])
                    posCtrls[[i]][match(names(wa), tolower(namePos))] <-
                        wa
                } 
                posCtrls[[i]]
            })
    } # oneWay
    ## negative controls:
    aux <- c(1:nrChannels)[!emptyOrNA(negControls)]
    if(any(aux))
        negCtrls[aux] <- lapply(aux, function(i)
                                as.numeric(findControls(negControls[i], wAnno)))
    AllControls <- list(posCtrls = posCtrls,
                        negCtrls = negCtrls,
                        actCtrls = actCtrls,
                        inhCtrls = inhCtrls)
    return(AllControls)
}



## Auxiliary function to split the controls according to the assay plate
ctrlsPerPlate <- function(controls, nrWells)
{
    plate <- ((nrWells - 1) + controls)%/%nrWells 
    cpP <- split(controls, plate) 
    return(cpP)
}



## Auxiliary function to calculate the dynamic range - called by 'getDynamicRange' function
dynRange <- function(z, p1, p2)
{
    abs(mean(as.matrix(z)[p1,] , na.rm=TRUE) - mean(as.matrix(z)[p2,], na.rm=TRUE))
}



## Auxiliary function to calculate the Z'-factor - called by 'getZfactor' function
zfacFun <- function(z, zneg, locationFun, spreadFun)
{
    1-3*(spreadFun(z, na.rm=TRUE) +
         spreadFun(zneg, na.rm=TRUE))/(abs(locationFun(z, na.rm=TRUE) -
                         locationFun(zneg, na.rm=TRUE)))
}






