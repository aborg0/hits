## Functions for plate-by-plate normalization:
## perPlateScaling
## controlsBasedNormalization

## ===========================================================
## Auxiliary functions

funOperator=function(a,b, op) {
op = get(op, mode="function")
op(a,b)
}

POC = function(a, pos, plate, replicate, channel,...){

if(all(is.na(a[pos]))) stop(sprintf("No values for positive controls were found in plate %s, replicate %s, channel %d! Please use a different normalization function.", plate, replicate, channel))

100*a/mean(a[pos], na.rm=TRUE)
}

NPI = function(a, pos, neg, plate, replicate, channel){
if(all(is.na(a[pos])) | all(is.na(a[neg]))) stop(sprintf("No values for positive or/and negative controls were found in plate %s, replicate %s, channel %d! Please use a different normalization function.", plate, replicate, channel))

(mean(a[pos], na.rm=TRUE) - a)/(mean(a[pos], na.rm=TRUE) - mean(a[neg], na.rm=TRUE))
}


## ===========================================================

## 		----	perPlateScaling ------
##
## For each plate, calculates the ratio between each measurement and
## a certain plate intensity statistic (e.g. for scaleByPlateMean,
## the average intensity over the wells containing 'sample' or 'negative' controls).
##
## if data are in "additive" scale
## subtract by the per-plate correction factor
## instead of dividing by it.
##
## stats - character of length 1 giving the name of the plate intensity statistic to calculate. Options are: "mean", "median", "shorth" and "negatives"

perPlateScaling <- function(object, scale, stats="median", negControls){

  funArgs <- list(na.rm=TRUE)

  if(stats=="shorth") funArgs$tie.action="min"


  op <- ifelse(scale=="additive", "-", "/") 
  statFun <- if(stats=="negatives") median  else get(stats, mode="function")


  xnorm <- Data(object)

  d <- dim(xnorm)
  nrWpP <- prod(pdim(object))
  nrPlates <- max(plate(object))
  nrSamples <- d[2]
  nrChannels <- d[3]

  wellAnnotation <- as.character(wellAnno(object))

  for(p in 1:nrPlates) {
    plateInds <- (1:nrWpP)+nrWpP*(p-1)
    wAnno = wellAnnotation[plateInds]
    inds <- (wAnno=="sample")
    for(ch in 1:nrChannels){

        if(stats=="negatives"){
          inds <- if(!(emptyOrNA(negControls[ch]))) findControls(negControls[ch], wAnno) else integer(0)

          if(!length(inds)) stop(sprintf("No negative controls were found in plate %s, channel %d! Please, use a different plate normalization method.", p, ch))
        }

        for(r in 1:nrSamples) {
         if(!all(is.na(xnorm[plateInds, r, ch]))) { 

          if(all(is.na(xnorm[plateInds, r, ch][inds])))
              stop(sprintf("No value for %s were found in plate %d, replicates %d, channel %d! Please %s", ifelse(stats=="negatives","negative controls", "samples"), p, r, ch, ifelse(stats=="negatives", "use a different plate normalization method.", "also flag the values for the controls in this plate!"))) 

          xnorm[plateInds,r,ch] <- funOperator(xnorm[plateInds, r, ch], do.call(statFun, c(list(x=xnorm[plateInds, r, ch][inds]), funArgs)), op)
      }#not all empty
  }# r
}# ch
       }# p
  Data(object) <- xnorm
  return(object)
}



## =========================================================================
##               ----- controlsBasedNormalization -----
## 
## General function that allows the following controls-based plate normalizations:
##       'POC' - Percent of control - determines the percentage of control, as the ratio between the 
## raw measurement and the mean of the measurements in the positive controls in an antagonist assay.
##
##       'NPI' - Normalized Percent Inhibition: for each plate, subtracts each measurement from the mean of the positive controls, and divides the result by the difference between the mean of positive and negative controls (plate dynamic range), in an antagonist assay.

controlsBasedNormalization <- function(object, method, posControls, negControls){

  xnorm <- Data(object)

  d <- dim(xnorm)
  nrWpP <- prod(pdim(object))
  nrPlates <- max(plate(object))
  nrSamples <- d[2]
  nrChannels <- d[3]

  wellAnnotation <- as.character(wellAnno(object))
  fun <- get(method, mode="function")

  for(p in 1:nrPlates) {
    plateInds <- (1:nrWpP)+nrWpP*(p-1)
    wAnno <- wellAnnotation[plateInds]

      for(ch in 1:nrChannels) {
        if(!(emptyOrNA(posControls[ch]))) pos <- findControls(posControls[ch], wAnno) else pos <- integer(0)
        if(!(emptyOrNA(negControls[ch]))) neg <- findControls(negControls[ch], wAnno)  else neg <- integer(0)
        if(method == "POC") {
           if(!length(pos)) stop(sprintf("No positive controls were found in plate %s, channel %d! Please, use a different plate normalization method.", p, ch)) 
        } else {
	   if (!length(pos) | !length(neg)) stop(sprintf("No positive or/and negative controls were found in plate %s, channel %d! Please, use a different normalization function.", p, ch))
        }

	for(r in 1:nrSamples) 
         if(!all(is.na(xnorm[plateInds, r, ch])) )
            xnorm[plateInds, r, ch] = fun(xnorm[plateInds, r, ch], pos, neg, plate=p, replicate=r,channel=ch)

     } 
  }
  Data(object) <- xnorm
  return(object)
}


##-------- Bscore method (without plate variance adjustment) --------------------------
## B score: The residual (rijp) of the measurement for row i and column j on the p-th plate is obtained
## by fitting a two-way median polish:

## rijp = yijp - yijp_hat = yijp - (miu_hat + Rip_hat + Cjp_hat)

## For each plate p, the adjusted MADp is obtained from the rijp's.
## The variance adjustment step (Bscore = rijp/MADp) is omitted.


Bscore <- function(object, save.model=FALSE) {

  if(!inherits(object, "cellHTS")) stop("'object' should be of class 'cellHTS'.")
  
  if(!state(object)[["configured"]])
    stop("Please configure 'object' (using the function 'configure') before normalization.")


  ## data
  xdat <- Data(object)

  d <- dim(xdat)
  nrWpP <- prod(pdim(object))
  nrPlates <- max(plate(object))
  nrSamples <- d[2]
  nrChannels <- d[3]

  if(save.model) {
     rowcol.effects <- array(as.numeric(NA), dim=d)
     overall.effects <- array(as.numeric(NA), dim=c(nrPlates, d[2:3]))
  }


  isSample <- (wellAnno(object)=="sample")

  for(p in 1:nrPlates) {
    # use only sample wells for the fit:
    plateInds <-(1:nrWpP)+nrWpP*(p-1)
    samples = isSample[plateInds]

    for(r in 1:nrSamples)
      for(ch in 1:nrChannels) {
#       y must be a numeric matrix with "plate rows" in rows and "plate columns" in columns:
        y <- ysamp <- xdat[plateInds, r, ch]
        if(!all(is.na(y))) {
        ysamp[!samples]=NA
        ysamp <- matrix(ysamp,
            ncol=pdim(object)["ncol"], nrow=pdim(object)["nrow"], byrow=TRUE)
        y = matrix(y,
            ncol=pdim(object)["ncol"], nrow=pdim(object)["nrow"], byrow=TRUE)
        m = medpolish(ysamp, eps = 1e-5, maxiter = 200, trace.iter=!TRUE, na.rm = TRUE)

## apply the model to all the plate wells and obtain the residuals rijp
## replace NA by zero:
  isNArow = is.na(m$row)
  isNAcol = is.na(m$col)
  isNA = outer(isNArow, isNAcol, "*")
  m$row[isNArow]=0
  m$col[isNAcol]=0
  rowcol = outer(m$row, m$col, "+")

  res = y - (m$overall + rowcol) 

# if the effect is NA in both column and row elements, restore the NA value:
  if (sum(isNA)) rowcol[as.logical(isNA)] = NA
    #res is a matrix plate row * plate column
    xdat[plateInds, r, ch] <- as.vector(t(res))

  if (save.model) {
      rowcol.effects[plateInds,r,ch] <- as.vector(t(rowcol))
      #residuals[,p,r,ch] = as.vector(t(m$residuals)) ## DON'T USE m$residuals, otherwise we'll have more NA 
      overall.effects[p,r,ch]<-m$overall
   }
  } 
} # ch
}# p

 if(save.model) {
   object@rowcol.effects <- rowcol.effects
   object@overall.effects <- overall.effects
 }

   Data(object) <- xdat
   object@state[["normalized"]] = TRUE
   validObject(object)
   return(object)
}


##                 ------- spatialNormalization ---------
##
## Fit a polynomial surface within each plate to the plate corrected intensities using local fit.
## uses a second degree polynomial (local quadratic fit)
##
## Inputs:
##### x -  cellHTS object
##### model  - fit the polynomial surface using robust "locfit" or "loess". The default is "locfit".
##### smoothPar - the parameter which controls the degree of smoothing (corresponds to 'span' argument of loess, or to the parameter 'nn' of 'lp' of locfit). The default is smoothPar = 0.6
##### save.model - should the fitted values be saved? Default=FALSE. If TRUE, the fitted values are stored in the slot 'rowcol.effects'. 
## -------------------------------------------------------------

spatialNormalization <- function(object, model="locfit", smoothPar=0.6, save.model=FALSE){
   
  if(!inherits(object, "cellHTS")) stop("'object' should be of 'cellHTS' class.")
 
  if(!state(object)[["configured"]])
    stop("Please configure 'object' (using the function 'configure') before normalization.")



  ## acts on slot 'assayData'
  xnorm <- Data(object)

  if (model=="locfit")  require("locfit") || stop("Package 'locfit' was not found and
needs to be installed.")


  d <- dim(xnorm)
  nrWpP <- prod(pdim(object))
  nrPlates <- max(plate(object))
  nrSamples <- d[2]
  nrChannels <- d[3]

  rowcol.effects <- array(as.numeric(NA), dim=d)
  posn <- 1:nrWpP

## Map the position in the plates into a (x,y) coordinates of a cartesian system having its origin at the centre of the plate
  row <- 1 +(posn-1) %/% pdim(object)[["ncol"]]
  col <- 1 + (posn-1) %% pdim(object)[["ncol"]]
  centre <- 0.5 + c(pdim(object)[["ncol"]]/2, pdim(object)[["nrow"]]/2) 
  xpos <- col - centre[1]
  ypos <- centre[2] - row

  wAnno <- wellAnno(object)

  for(p in 1:nrPlates) {
    # use only sample wells for the fit:
    plateInds <- (1:nrWpP)+nrWpP*(p-1)
    samples <- (wAnno[plateInds]=="sample")

    for(r in 1:nrSamples)
      for(ch in 1:nrChannels){
        y <- ysamp <- xnorm[plateInds, r, ch]
        if(!all(is.na(y))) {
          ysamp[!samples] <- NA
          y <- yf <- data.frame(y=y, xpos=xpos, ypos=ypos)
          yf$xpos <- factor(xpos)
          yf$ypos <- factor(ypos)
          ysamp <- yfsamp <- data.frame(y=ysamp, xpos=xpos, ypos=ypos)
          yfsamp$xpos <- factor(xpos)
          yfsamp$ypos <- factor(ypos)

          ## Correct for spatial effects inside the plate using robust local fit
          posNAs <- is.na(ysamp$y)
          ysamp_complete <- ysamp[!posNAs,]
          yfsamp_complete <- yfsamp[!posNAs,]
          m = switch(model,
            "loess" = loess(y ~ xpos + ypos, data=ysamp_complete, normalize=TRUE, span=smoothPar,
                          control = loess.control(iterations=40)),
            "locfit" = locfit(y ~ lp(xpos, ypos, nn=smoothPar, scale=TRUE),
                          data = yfsamp_complete, lfproc=locfit.robust),
            stop(sprintf("Invalid value '%s' for argument 'model'", model))
          )

        # apply the model to all the plate wells and obtain the residuals rijp
        predx <- switch(model,
          "loess" <- predict(m, newdata=y),
          "locfit" <- predict(m, newdata=yf))

        #replace predicted NA by 0 to avoid having extra NA entries in xn:
        isNA <- is.na(predx)
        predx[isNA] <- 0  # safe, because we are going to perform a subtraction
        xnorm[plateInds,r,ch] <- y$y - predx
        # put back to NAs
        predx[isNA] <- NA  

        rowcol.effects[plateInds,r,ch] = predx
        }#if all is.na

        }#for channel
    }#for replicate

  if (save.model) {
     object@rowcol.effects <- rowcol.effects
     ## reset overall.effects to default of 'cellHTS' class to avoid problems in validity:
     object@overall.effects <-new("cellHTS")@overall.effects
   }

  object@state["normalized"] = TRUE
  Data(object) <- xnorm
  validObject(object)
  return(object)
} #end function


