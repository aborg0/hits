## (C) Ligia P. Bras 2006
##
## 'object' is a 'cellHTS' object
## 'ar' is the aspect ratio for the image plot (i.e. number columns
##    divided by the number of rows)
## 'zrange' is the range of values to be mapped into the color scale.
##   If the argument "zrange" is missing, it will be set to zrange=range(scores(object), na.rm=TRUE)
## tool tips added with the help of Florian Hahne (August 2006)

imageScreen <- function (object, ar=3/5, zrange, map=FALSE, anno, channel = 1) {

  if(!state(object)["scored"])
    stop("Please score 'object' (using the function 'summarizeReplicates').")

  ## Determine the number of columns and rows for the image plot,
  ## given the aspect ratio 'ar' provided by the user
  sc.true <- sc <- as.vector(Data(object)[, channel, 1])
  nrPlates = max(plate(object))
  nrCol = ceiling(sqrt(ar*nrPlates))
  nrRow = ceiling(nrPlates/nrCol)

  nrWells = prod(pdim(object))
  pos = 1:nrWells
  pRow = pdim(object)[1] 
  pCol = pdim(object)[2]

  ## Get the y coordinate for the image plot (the center is in the upper-left corner of the plate)
  ypos = (pRow+1) - (1+(1:nrWells-1)%/% pCol)

  ## Create big matrices for the image plot (plates will be added horizontally)
  Nrow = nrRow*(pRow+1)-1
  Ncol = nrCol*(pCol+1)-1
  mat.true <- mat <- matan <- matrix(as.numeric(NA), ncol=Ncol, nrow=Nrow)

  ## Check if zrange was given as an argument
  if (missing(zrange)) {
    ## set default values
    zrange <- range(sc.true, na.rm=TRUE)
  }

  if (!missing(anno)) {
    if(length(anno)!=length(sc.true) & map)
       stop("Argument 'anno' is not valid! It should be a vector with the same length as the total number of features 'dim(object)[1]'.")
  }else{## if !missing anno
    if(object@state[["annotated"]]){
       if ("GeneSymbol" %in% names(fData(object)))
         anno <- fData(object)$GeneSymbol
       else
         anno <- geneAnno(object)
    }else{##else if annotated
       anno <- position(object) #rep(sprintf("position %d", pos), nrPlates)
    }##else annotated
  }## else !missing anno

  anno <- paste(anno, " (plate ", rep(1:nrPlates, each=nrWells), ", well ", well(object), ")", sep="")

  ## replace NA by zero (because it will be neutral for the current analysis)
  sc[is.na(sc)] <- 0

  ## Cap the values outside the dynamic range defined by the user (zrange):
  sc[sc<zrange[1]] = zrange[1]
  sc[sc>zrange[2]] = zrange[2]

  if (prod(zrange)<0) {
    ## map the z-values to interval [0,1], with 0.5 corresponding to z=0
    scunit = (sc/max(abs(zrange)) + 1) / 2
    reverseMap = function(x) { (x*2-1)*max(abs(zrange)) }
    rdbu  = rev(brewer.pal(11, "RdBu"))[c(1:5, rep(6,3), 7:11)] ## give a little more room to white
    colrs = colorRampPalette(rdbu)(256)
  } else {
    ## map the z-values to interval [0,1], with 0 corresponding to minimum
    ## and 1 to maximum of the range
    scunit = (sc-zrange[1])/diff(zrange)
    reverseMap = function(x) { x*diff(zrange)+zrange[1] }
    colrs = colorRampPalette(brewer.pal(9, "Greys"))(256)
  }

  ## fill the matrices and leave spacers to separate the plates
  for(r in 1:nrRow) {
    onerow.true <- onerow <- onerowan <- matrix(NA, nrow=pRow, ncol=pCol*nrCol+nrCol-1)
    for(c in 1:nrCol) {
      p <- nrCol * (r-1) + c 
      if(p <= nrPlates){
        xsc <- scunit[nrWells * (p-1) + c(1:nrWells)]
        xan <- anno[nrWells * (p-1) + c(1:nrWells)]
        xsc.true <- sc.true[nrWells * (p-1) + c(1:nrWells)]
      }else{
        xsc <- xan <- xsc.true <- rep(NA, nrWells)
      }
      xsc.true <- matrix(xsc.true[order(ypos)], nrow = pRow, ncol=pCol, byrow=TRUE)
      xsc <- matrix(xsc[order(ypos)], nrow = pRow, ncol=pCol, byrow=TRUE)
      xan <- matrix(xan[order(ypos)], nrow = pRow, ncol=pCol, byrow=TRUE)
      sel <- pCol * (c-1) + c(1:pCol) + (c-1) * (c>1)
      onerow[,sel] <- xsc
      onerowan[,sel] <- xan
      onerow.true[,sel] <- xsc.true
    }
    sel2 <- (Nrow+1-r*pRow-(r-1)*(r>1)):(Nrow-pRow*(r-1)-(r-1)*(r>1))
    mat[sel2,] <- onerow
    matan[sel2, ] <- onerowan
    mat.true[sel2,] <- onerow.true
  }

  ## Include the color scale bar
  ## Just to make sure that we will have enough columns for the color bar
  extraRows <- max(10, ceiling(0.15*Nrow))
  newmat.true <- newmat <- newmatan <- matrix(NA, nrow =Nrow+extraRows, ncol = Ncol)

  ## add the color scale bar (size depends on the plate size and number)
  xbar <- seq(0, 1, length=7)
  xval <- round(reverseMap(xbar), 1)
  nColBar <- 1 + (Ncol>25) + (Ncol>100)
  nRowBar <- 1 + (Nrow>50) + (Nrow>100) + (Nrow>150)
  yBar.start <- ifelse(extraRows==10, 0.4 * extraRows, 0.6 * extraRows)
  newmat[yBar.start + (1:nRowBar), 1:(length(xbar)*nColBar)] <- matrix(data=rep(xbar, each = nColBar,
                               times=nRowBar), nrow = nRowBar, ncol=length(xbar)*nColBar, byrow=TRUE)
  newmat[extraRows + (1:Nrow),] = mat
  newmat <- cbind(NA, newmat, NA) # add extra empty columns and rows at the edges
  newmat <- rbind(NA, newmat, NA) 


if (map) {
  newmatan[extraRows + (1:Nrow),] = matan
  newmat.true[extraRows + (1:Nrow),] = mat.true
  newmatan <- cbind(NA, newmatan, NA)
  newmatan <- rbind(NA, newmatan, NA)
  newmat.true <- cbind(NA, newmat.true, NA)
  newmat.true <- rbind(NA, newmat.true, NA)
}

  nc <- Ncol+2
  nr <- Nrow+extraRows+2
  par(mar=rep(0,4))
  image(1:nc, 1:nr, z=t(newmat), zlim=c(0,1), axes=FALSE, col=colrs, add = FALSE,
        ylab="", xlab="")
  text(seq(0.5*nColBar+1.5, length(xbar)*nColBar+1, by=nColBar), y=ifelse(extraRows==10, 3, 0.25*extraRows),
       offset=0, cex = 1, srt=90, labels=c(paste(c("< ", rep("", length(xval)-2), ">"), xval, sep="")))

  if(map){
    xlim = c(0, nc)
    ylim = c(0, nr)
    dx <- 1
    fw <- diff(xlim)
    fh = diff(ylim)
    w <- h <- 7*72
    u2px = function(x) (x - xlim[1])/fw * w
    u2py = function(y) (y - ylim[1])/fh * h
    x0 <- (1:(prod(nc,nr))-1) %% nc
    y0 <- (1:(prod(nc,nr))-1) %/% nc
    x1 <- x0+dx
    y1 <- y0+dx
    #newmat[(yBar.start + (1:nRowBar))+1, (1:(length(xbar)*nColBar))+1] <- NA
    rowSpacer <- which(rowSums(is.na(newmatan[nr:1,]))==nc)
    colSpacer <- which(rowSums(t(is.na(newmatan[nr:1,])))==nr)
    nnc <- nc-length(colSpacer) 
    nnr <- nr-length(rowSpacer)
    iplate <- rep((0:(nnc-1))%/%pCol+1, nnr) + rep(seq(0, nrPlates-1, by=nrCol),
                                                   each=prod(pRow, pCol, nrCol))  
    tit <- paste(as.vector(t(newmatan[nr:1,])), 
            sprintf(": score=%g", signif(as.vector(t(newmat.true[nr:1,])),3)), sep="")


    imap <- matrix(c(u2px(x0), u2py(y0), u2px(x1), u2py(y1)), ncol=4, nrow=length(x0), byrow=FALSE)
 
#    NArows = (rep(rowSpacer, each=nc)-1)*nc + rep(1:nc, length(rowSpacer))
#    NAcols =  nc*(0:(nr-1)) + rep(colSpacer, each=nr)
#    imap <- imap[-unique(c(NArows, NAcols)), ]
    ## remove spacers
    Spacers <- array(TRUE, dim=dim(newmat))
    Spacers[rowSpacer,] <- FALSE
    Spacers[,colSpacer] <- FALSE
    Spacers <- as.vector(t(Spacers))

    #empty <- regexpr("NA: score=NA", imap[,5])>0
    isEmpty <- which(iplate>nrPlates)
    if (length(isEmpty)) {
      imap <- imap[Spacers,][-isEmpty,] 
      tit <- tit[Spacers][-isEmpty] 
      iplate <- iplate[-isEmpty]
    } else {
      imap <- imap[Spacers,]
      tit <- tit[Spacers] 
    }

   imap[] <- as.integer(imap)
 #   return(myImageMap(imap[,1:4], list(TITLE=imap[,5], href=paste(iplate[-isEmpty], "index.html", sep="/")), "imageScreen.png"))
    return(list(obj=imap, tag=list(TITLE=tit, HREF=paste(iplate, "index.html", sep="/"))))
  }
}
