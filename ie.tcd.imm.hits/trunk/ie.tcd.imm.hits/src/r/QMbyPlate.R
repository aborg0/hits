
myImageMap <- function(object, tags, imgname) {

  if(!is.matrix(object)||ncol(object)!=4)
    stop("'object' must be a matrix with 4 columns.")


  len <- lapply(tags, length)
  if(any(len!=nrow(object))) stop(sprintf("Elements of the list 'tag' must have a length equal to the number of rows of 'object' (%g).", nrow(object)))

  mapname <- paste("map", gsub(" |/|#", "_", imgname), sep="_")
  #out <- paste("<IMG SRC=\"", imgname, "\" USEMAP=#", mapname, " BORDER=2>", "<MAP NAME=\"", mapname, "\">", sep="")
   outin <- paste("<IMG SRC=\"", imgname, "\" USEMAP=#", mapname, " BORDER=2>", "<MAP NAME=\"", mapname, "\">", sep="")

   stopifnot(names(tags) == c("TITLE", "HREF"))

   out <- lapply(1:nrow(object), function(i) { 

       paste(paste("<AREA SHAPE=\"rect\" COORDS=\"", paste(object[i,], collapse=","),
                "\"", sep=""), paste(" ", paste(names(tags), "=\"", 
                c(tags[["TITLE"]][i], tags[["HREF"]][i]), 
                "\"", sep=""), collapse=" "), "\">", sep="")
          } 
       ) 

## add all together:
#"
out <- paste(unlist(out), collapse="")
out <- paste(outin, out, sep="")

} #myImageMap







## ---------------------------------------------------------------------------
QMbyPlate <- function(platedat, pdim, name, basePath, subPath, genAnno, mt,
                      plotPlateArgs, brks, finalWellAnno, activators, inhibitors, positives, negatives,
                      isTwoWay, namePos, wellTypeColor, plateDynRange, plateWithData, repMeasures, channels=paste("Channel", 1:dim(platedat)[4], sep=" ")) {

  fn  = file.path(subPath, "index.html")
  con = file(file.path(basePath, fn), "w")
  on.exit(close(con))

  writeheader(paste("Quality report for", name), 1, con)

#  stopifnot(dim(platedat)[2]==1)
#  stopifnot(dim(platedat)[1]==length(wAnno))

  ## dimensions
  d <- dim(platedat)
  nrWells = prod(pdim)
  nrChannel = d[4]
  maxRep = d[3]

  ## which of the replicate plates has not just all NA values
  whHasData <- list()
  for (ch in 1:nrChannel) whHasData[[ch]] <- which(plateWithData[,,ch])

  nrRepCh = sapply(whHasData, length)

  ## Checks whether the number of channels has changed (e.g. normalized data)
  hasLessCh = any(dim(finalWellAnno)!=dim(platedat))

  ## NOTE: 'subPath' corresponds to the plate number!
  ## Get well positions for current controls and for samples
  ppos <- pneg <- pact <- pinh <- list()
  if(isTwoWay) {
     for (ch in 1:nrChannel){
       pneg[[ch]] <- negatives[[ch]][[as.character(subPath)]]-(subPath-1)*(nrWells) # correct to be in range 1:nrWells
       pact[[ch]] <- activators[[ch]][[as.character(subPath)]]-(subPath-1)*(nrWells) # correct to be in range 1:nrWells
       pinh[[ch]] <- inhibitors[[ch]][[as.character(subPath)]]-(subPath-1)*(nrWells) # correct to be in range 1:nrWells 
     }
  } else {  #oneWay
    for (ch in 1:nrChannel){
       pneg[[ch]] <- negatives[[ch]][[as.character(subPath)]]-(subPath-1)*(nrWells) # correct to be in range 1:nrWells
       ppos[[ch]] <- lapply(names(positives[[ch]]), function(i) positives[[ch]][[i]][[as.character(subPath)]]-(subPath-1)*(nrWells)) 
       names(ppos[[ch]]) <- names(positives[[ch]])
    }
  }

  samples <- which(mt==which(names(wellTypeColor)=="sample"))


  ## summary of the quality metrics in 'qm' to be returned by this function:
  qmsummary <- vector("list", length=nrChannel)
  names(qmsummary) = sprintf("Channel %d", 1:nrChannel)

  ## Create table with per-plate quality metrics
  for (ch in 1:nrChannel) {
    nrRep = nrRepCh[ch]
    #count = 0
   # --------------------------------------------------------------
   # 1) create summary table from dynamic range:
    d <- length(plateDynRange)*(maxRep + 1)
    qm = data.frame(metric=I(character(d)), value=NA, comment=I(character(d)))

    for(i in 1:length(plateDynRange)) {

      if(names(plateDynRange)[i]=="pos" & length(plateDynRange)==1) pn = "" else pn = sprintf("'%s'",names(plateDynRange)[i])
      qm$metric[(i-1)*(maxRep+1)+(1:maxRep)] = I(sprintf("Dynamic range %s (replicate %s)",pn , 1:maxRep))
      qm$metric[(i-1)*(maxRep+1)+(maxRep+1)] = I(sprintf("Dynamic range %s",pn))
      qm$value[(i-1)*(maxRep+1)+(1:maxRep)] <- round(plateDynRange[[i]][1, 1:maxRep, ch],2)
      qm$value[(i-1)*(maxRep+1)+(maxRep+1)] = round(plateDynRange[[i]][1,maxRep+1, ch],2)

      hasNoVal <- is.na(plateDynRange[[i]][1,1:maxRep,ch]) 

      if(any(hasNoVal)) {
        if(all(is.na(plateDynRange[[i]][1,,ch]))) {
          qm$comment[(i-1)*(maxRep+1) + (1:(maxRep+1))] <- I(sprintf("No controls ('%s' and/or 'neg') were found.", ifelse(names(plateDynRange)[i] %in% c("activators", "inhibitors"), names(plateDynRange)[i], "pos"))) 
        } else {
 
          a <- intersect(hasNoVal, whHasData[[ch]]) 
          if(length(a)) qm$comment[(i-1)*(maxRep+1) + a] <- I("No available values for one of the controls")
          b <- setdiff(1:maxRep, whHasData[[ch]])
          if(length(b)) qm$comment[(i-1)*(maxRep+1) + b] <- I(paste(paste("Replicate", b, sep=" "), "is missing", sep=" "))
       } # else all(is.na...
    } #  any(hasNoVal)
  } # for i in 1:length(plateDynRange)


    ## ---------------------------------------------------------------------------
    ## 2. Correlation coefficient (just for samples wells)
    comm = ""    
    if(nrRep>1) { # subPath corresponds to the plate number
      cc1 = round(repMeasures$repStDev[subPath,ch],2)
      #if(nrRep==2 & nrRep==maxRep) {
      if(maxRep==2) {
        cc2 = round(repMeasures$corrCoef[subPath,ch],2)  
      } else {
        cc2 = paste(round(repMeasures$corrCoef.min[subPath,ch],2), round(repMeasures$corrCoef.max[subPath,ch],2), sep=" - ")
      }
    } else {
       cc1 <- cc2 <- as.numeric(NA)
       comm = sprintf("%d replicate%s", nrRep, ifelse(nrRep, "", "s"))
    }
   qm = rbind(qm, data.frame(metric=I("Repeatability standard deviation"), value=cc1, comment=I(comm)))
   qm = rbind(qm, data.frame(metric=I(sprintf("Spearman rank correlation %s", ifelse(maxRep==2,"","(min - max)"))), value=cc2, comment=I(comm)))
   
   if (exists("qmplate")) qmplate <- cbind(qmplate, value=qm$value, comment=qm$comment) else qmplate = qm
    ## store data in qmsummary 
    qmsummary[[ch]] = qm$value
    names(qmsummary[[sprintf("Channel %d", ch)]]) = qm$metric 
  } # for ch 
  qmplate[is.na(qmplate)] <- I("--")
  writeHTMLtable(qmplate, con=con, center=TRUE, extra=channels)

  ## ------------------  Color legend for each channel ----------------------------------
  ## For the original configuration plate corrected by the screen log information:
  wellCount = data.frame(matrix(NA, ncol = nrChannel, nrow = 2))
  names(wellCount) = sprintf("Channel %d", 1:nrChannel)
  mtt <- vector("list", length = nrChannel)
  iwells <- match(c("flagged", "empty", "other", "controls", "pos", "neg", "act", "inh"), names(wellTypeColor))
  names(iwells) <- c("flagged", "empty", "other", "controls", "pos", "neg", "act", "inh")

#  for(i in c("flagged", "empty", "other", "controls", "pos", "neg", "act", "inh")) 
#        assign(paste("i", toupper(substr(i,1,1)), sep=""), which(names(wellTypeColor)==i))


  if (hasLessCh & nrChannel==1) {
    ## The color code must take into account the common entries between channels and replicates 

    mtt[[1]] <- mt
    fwa <- matrix(finalWellAnno, ncol = prod(dim(finalWellAnno)[3:4]))
    mtrep <- apply(fwa, 2, function(u) match(u, names(wellTypeColor)))
    ## include the controls that were not annotated as "neg" or "pos":
    if (isTwoWay) {
    mtrep[pact[[1]],] [which(is.na(mtrep[pact[[1]],]))] <- iwells[["act"]]
    mtrep[pinh[[1]],] [which(is.na(mtrep[pinh[[1]],]))] <- iwells[["inh"]]
    } else {
    mtrep[unlist(ppos[[1]]),] [which(is.na(mtrep[unlist(ppos[[1]]),]))] <- iwells[["pos"]]
    }
    mtrep[pneg[[1]],] [which(is.na(mtrep[pneg[[1]],]))] <- iwells[["neg"]]

    ## replace the remaining NA positions by "other" (these corresponds to wells that
    ## although annotated as controls in the configuration file, don't behave as
    ## controls in the current channel
    mtrep[which(is.na(mtrep))] <- iwells[["other"]]
    aa <- apply(fwa, 2, function(u) sum(u=="flagged"))
    aa <- order(aa, decreasing=TRUE) # position 1 contains the replicate with more flagged values
    nrWellTypes <- sapply(seq(along=wellTypeColor), function(i) sum(mtrep[,aa[1]]==i, na.rm=TRUE))


    ## flagged wells
    wellCount[1,1] <- paste(sprintf("flagged: %d", nrWellTypes[iwells[["flagged"]]]), collapse=", ")
    ## all the other wells, except controls
    wellCount[2, 1] <- paste(sprintf("<FONT COLOR=\"%s\">%s: %d</FONT>", wellTypeColor[-c(iwells[["flagged"]],iwells[["controls"]])], names(wellTypeColor)[-c(iwells[["flagged"]],iwells[["controls"]])], nrWellTypes[-c(iwells[["flagged"]], iwells[["controls"]])]), collapse=", ")
    ## so "flagged" always wins over "pos", "neg" or "sample"
    mtt[[1]][is.na(mtt[[1]])] <- apply(mtrep[is.na(mtt[[1]]),, drop=FALSE], 1, max) 
    ## so "controls" always wins over "pos" or "neg" or "act" or "inh" or "sample"
    mtt[[1]][!is.na(mtt[[1]])] <- apply(mtrep[!is.na(mtt[[1]]),, drop=FALSE], 1, max)

  } else { ## if hasLessCh

    for (ch in 1:nrChannel) {
      mtt[[ch]] <- mt
      mtrep = apply(finalWellAnno[,,,ch, drop=FALSE], 3, match, names(wellTypeColor))

      ## include the controls that were not annotated as "neg" or "pos":
      ## correct 'pos' controls just for one-way assays
      if (!isTwoWay) {
         if (length(unlist(ppos[[ch]]))) {
           mtrep[unlist(ppos[[ch]]),][which(is.na(mtrep[unlist(ppos[[ch]]),]))] <- iwells[["pos"]]
         } else { ## if length pos
         ## replace possible wells annotated as "pos" by NA, because they shouldn't be considered as a positive control for this channel:
           if (any(mtt[[ch]] %in% iwells[["pos"]])) {
             mtrep[mtt[[ch]] %in% iwells[["pos"]],]=NA
             mtt[[ch]][mtt[[ch]] %in% iwells[["pos"]]]=NA 
           } ## if any
         } ## else length pos
      }else{ ## if !isTwoWay

    ## include the controls that were not annotated as "act" or "neg", but only if they
    ## should be regarded as such in this channel
    if (length(pact[[ch]])) {
      mtrep[pact[[ch]],] [which(is.na(mtrep[pact[[ch]],]))]=iwells[["act"]]
    }else{## if length act
       if (any(mtt[[ch]] %in% iwells[["act"]])) {
        mtrep[mtt[[ch]] %in% iwells[["act"]],] <- NA
        mtt[[ch]][mtt[[ch]] %in% iwells[["act"]]] <- NA 
      } ## if any
    } ## else length act
    if (length(pinh[[ch]])) {
      mtrep[pinh[[ch]],] [which(is.na(mtrep[pinh[[ch]],]))]=iwells[["inh"]]
    }else{## if length inh
       if (any(mtt[[ch]] %in% iwells[["inh"]])) {
        mtrep[mtt[[ch]] %in% iwells[["inh"]],] <- NA
        mtt[[ch]][mtt[[ch]] %in% iwells[["inh"]]] <- NA 
      } ## if any
    } ## else length inh
   }##else if !isTwoWay

      ## for the negative controls
     if (length(pneg[[ch]])) {
       mtrep[pneg[[ch]],] [which(is.na(mtrep[pneg[[ch]],]))] <- iwells[["neg"]] 
     } else { ## if length neg
      if (any(mtt[[ch]] %in% iwells[["neg"]])) {
        mtrep[mtt[[ch]] %in% iwells[["neg"]],] <- NA
        mtt[[ch]][mtt[[ch]] %in% iwells[["neg"]]] <- NA 
      } ## if any
    } ## else length neg

      ## replace the remaining NA positions by "other" (these corresponds to wells that
      ## although annotated as controls in the configuration file, don't behave as controls
      ## in the current channel
      mtrep[which(is.na(mtrep))] <- iwells[["other"]]
      aa <- apply(finalWellAnno[,,,ch, drop=FALSE], 3, function(u) sum(u=="flagged"))
      aa <- order(aa, decreasing=TRUE)
      nrWellTypes = sapply(seq(along=wellTypeColor), function(i) sum(mtrep[,aa[1]]==i, na.rm=TRUE))

      ## flagged wells
      wellCount[1,ch] <- paste(sprintf("flagged: %d", nrWellTypes[iwells[["flagged"]]]), collapse=", ")
      ## the other wells, except controls
      wellCount[2, ch] = paste(sprintf("<FONT COLOR=\"%s\">%s: %d</FONT>",
                 wellTypeColor[-c(iwells[["flagged"]], iwells[["controls"]])], names(wellTypeColor)[-c(iwells[["flagged"]],iwells[["controls"]])],
                 nrWellTypes[-c(iwells[["flagged"]], iwells[["controls"]])]), collapse=", ")
      ## so "flagged" always wins over "pos", "neg" or "sample" or "act" or "inh"
      mtt[[ch]][is.na(mtt[[ch]])]=apply(mtrep[is.na(mtt[[ch]]),, drop=FALSE], 1, max)
    }## for channel
  }## else hasLessCh


  ##  ------------------  Make plots ----------------------------------
  ## Create a dataframe for the plots of each channel
  cat("<BR>\n", file=con)
  cat("<BR>\n", file=con)

  plotTable <- data.frame(matrix(data = NA, nrow = 0, ncol = nrChannel + 1))
  names(plotTable) = c("", channels)

  plsiz = 4
  for (ch in 1:nrChannel) {
    nrRep = nrRepCh[ch]
    ## scatterplot
    ## plot title
    plotTable[1, ch+1] = "<H5 align=center><FONT color=#494F8A>SCATTERPLOT BETWEEN REPLICATES</FONT></H5>\n"
    count = 1

    ## ------------- Scatter plot between replicates ----------------------------
    if(nrRep==2) {
      makePlot(file.path(basePath, subPath), con=con,
               name=sprintf("scp_Channel%d", ch), w=plsiz, h=plsiz, fun = function() {
                 par(mai=c(0.9,0.9,0.2,0.2))
                 ylim=c(min(platedat[,,,ch], na.rm=TRUE), max(platedat[,,,ch], na.rm=TRUE))
                 plot(platedat[,,whHasData[[ch]][1],ch], platedat[,,whHasData[[ch]][2],ch], pch=16, cex=0.5,
                      ylim=ylim, xlab=paste("replicate", whHasData[[ch]][1], sep=" "), ylab=paste("replicate", whHasData[[ch]][2], sep=" "), col=wellTypeColor[mtt[[ch]]])
                 abline(a=0, b=1, col="lightblue")
               }, print=FALSE)

      ## color legend:
      wellLeg = paste(sprintf("<CENTER>%s</CENTER><BR>\n", wellCount[1,ch]),
        sprintf("<CENTER><em>Color legend: </em><br> %s</CENTER><BR>\n", wellCount[2,ch]), collapse="")

      plotTable[count + 1, ch+1] = sprintf("%s<CENTER><A HREF=\"%s\"><IMG SRC=\"%s\"/></A></CENTER>\n",
                 wellLeg, sprintf("scp_Channel%d.pdf", ch), sprintf("scp_Channel%d.png", ch))
 
    } else {## if nrRep
      plotTable[count + 1, ch+1] = sprintf("<CENTER>%d replicate(s): scatterplot omitted</CENTER>\n", nrRep)
	  tableOfPlots = "<!--Something starts here, the JS diagrams.-->";
	  #matrixOfData = matrix(ncol = nrRep, nrow=nrRep)
	  for (i in 1:nrRep)
	  {
		  tableOfPlots = paste(tableOfPlots,"<tr>", sep="")
		  for (j in nrRep)
		  {
			  #matrixOfData[i][j] = 
			  dataString = paste("var d = [",  paste("[", platedat[,,whHasData[[ch]][i],ch], ", ", platedat[,,whHasData[[ch]][j],ch], "]", sep="", collapse=","), "];\n", sep="")
			  tableOfPlots = paste(tableOfPlots,
				"<td><script id=\"source\" language=\"javascript\" type=\"text/javascript\">\n$(function () {", dataString,
			  "$.plot($(\"#placeholder\"), [\n        {\n            data: d,\n            points: { show: true}\n        },", "});\n</script></td>", sep="")
		  }
		  tableOfPlots = paste(tableOfPlots,"</tr>", sep="")
	  }
	  #plotTable[count + 1, ch+1] = sprintf("<CENTER><table>%s</table></CENTER>\n", tableOfPlots)
    }
    count = count + 1 

    ## ------------- Histograms (replicates) ----------------------------
    plotTable[count + 1, ch+1] = "<H5 align=center><FONT color=#494F8A>HISTOGRAM(S)</FONT></H5>\n"
    count= count + 1
    aa <- c(iwells[["pos"]], iwells[["neg"]], iwells[["act"]], iwells[["inh"]])
    aa <- aa[!is.na(aa)] 
    for (r in 1:maxRep) {
      plotTable[count+1, 1] = sprintf("<H4 align=left>Replicate %d</H4>\n", as.integer(r))
      if (r %in% whHasData[[ch]]){
         makePlot(file.path(basePath, subPath), con=con,
                 name=sprintf("hist_Channel%d_%02d",ch,r), w=plsiz, h=plsiz*.6, fun = function() {
                   par(mai=c(0.7,0.25,0.01,0.1))
                   hist(platedat[,,r,ch], xlab ="", breaks=brks[[ch]],
                        col = gray(0.95), yaxt = "n", main="")
                   rug(platedat[,,r,ch])
                   for(jj in aa) rug(platedat[,,r,ch][mtt[[ch]]==jj], col=wellTypeColor[jj])
                 }, print=FALSE)
        plotTable[count+1,ch+1] = sprintf("<CENTER><A HREF=\"%s\"><IMG SRC=\"%s\"/></A></CENTER>\n",
                   sprintf("hist_Channel%d_%02d.pdf",ch,r), sprintf("hist_Channel%d_%02d.png",ch,r)) 
      } else {
        plotTable[count + 1, ch+1] = sprintf("<CENTER>Replicate %d is missing</CENTER>\n", r)}
      count = count+1
    }## for r
  } ## for channel


  ## ------------- Plate plots (relicates) ----------------------------
  if(is.list(plotPlateArgs)) {
    plsiz = 4

    oldcount = count

    for (ch in 1:nrChannel) {
      char <- character(dim(platedat)[1])
      char[pneg[[ch]]] <- "N"
      if (isTwoWay) {
        char[pact[[ch]]] <- "A"
        char[pinh[[ch]]] <- "I"
      } else {
        char[unlist(ppos[[ch]])] <- "P" 
      }
      count = oldcount
      ## plot global title
      plotTable[count+1, ch+1] = "<H5 align=center><FONT color=#494F8A>PLATE PLOT(S)</FONT></H5>\n"

        ## plot title
        plotTable[count+2, 1] = "<H5 align=left>Standard deviation across replicates</H5>\n"

      ## platePlot of sd
      sdWithNA = function(x) {
        x = x[!is.na(x)]
        if(length(x)>0L) sd(x) else as.numeric(NA)
      }
      psd = apply(platedat[,,,ch,drop=FALSE], 1, sdWithNA)

      if(!all(is.na(psd))){

        if(is.null(plotPlateArgs$sdrange[[ch]]))
          plotPlateArgs$sdrange[[ch]]=c(0, quantile(psd, 0.95, na.rm=TRUE))

        pp <- makePlot(file.path(basePath, subPath), con=con,
                        name=sprintf("ppsd_Channel%d",ch), w=plsiz, fun = function() {
                          return(plotPlate(psd, nrow=pdim["nrow"], ncol=pdim["ncol"], na.action="xout",
                                           main="between replicate standard deviations",
                                           col=plotPlateArgs$sdcol, char=char,
                                           xrange=plotPlateArgs$sdrange[[ch]]) )
                        }, print=FALSE, isPlatePlot=TRUE)

      if(plotPlateArgs$map) {
        img <- myImageMap(object=pp$coord, tags=list(TITLE=paste(genAnno, ": sd=", signif(psd,3), sep=""),                                           HREF=rep(sprintf("ppsd_Channel%d.pdf", ch),                                       length(genAnno))), sprintf("ppsd_Channel%d.png", ch))

        plotTable[count+2, ch+1] = paste("<CENTER>", img, "</CENTER><BR>\n", sep="")
      } else {
        plotTable[count+2,ch+1] = sprintf("<CENTER><A HREF=\"%s\"><IMG SRC=\"%s\"/></A></CENTER>\n",
                   sprintf("ppsd_Channel%d.pdf",ch), sprintf("ppsd_Channel%d.png",ch)) 
      }


      } else {
        plotTable[count+2, ch+1] = sprintf("<CENTER>%d replicate(s): plate plot omitted</CENTER>\n",
                          nrRep)
      }#(!all(is.na(psd)))

      count = count + 2

      ## platePlot of intensities
      for (r in 1:maxRep) {
        plotTable[count+1, 1] = sprintf("<H4 align=left>Replicate %d</H4>\n", as.integer(r))
        if (r %in% whHasData[[ch]]){

          if(is.null(plotPlateArgs$xrange[[ch]]))
            plotPlateArgs$xrange[[ch]]=quantile(platedat[,,,ch], c(0.025, 0.975), na.rm=TRUE)

          pp <- makePlot(file.path(basePath, subPath), con=con,
                   name=sprintf("pp_Channel%d_%d",ch,r), w=plsiz, h=plsiz*0.66, fun = function() {
                     plotPlate(platedat[,,r,ch], nrow=pdim["nrow"], ncol=pdim["ncol"], na.action="xout",
                               main=sprintf("intensities for replicate %d", r),
                               col=plotPlateArgs$xcol, char=char,
                               xrange=plotPlateArgs$xrange[[ch]])
                   }, print=FALSE, isPlatePlot=TRUE)

          if(plotPlateArgs$map) {
            img <- myImageMap(object=pp$coord, tags=list(TITLE=paste(genAnno, ": value=",
                            signif(platedat[,,r,ch],3), sep=""), HREF=rep(sprintf("pp_Channel%d_%d.pdf", ch, r),
                                       length(genAnno))), sprintf("pp_Channel%d_%d.png", ch, r))
            plotTable[count+1,ch+1] =  paste("<CENTER>", img, "</CENTER><BR>\n", sep="")
          } else { #map

            plotTable[count+1,ch+1] = sprintf("<CENTER><A HREF=\"%s\"><IMG SRC=\"%s\"/></A></CENTER>\n",
                   sprintf("pp_Channel%d_%d.pdf",ch,r), sprintf("pp_Channel%d_%d.png",ch,r)) 
          }
         } else {## if r %in$ whHasData[[ch]]
          plotTable[count + 1, ch+1] = sprintf("<CENTER>Replicate %d is missing</CENTER>\n", r)
        }## else if r %in% whHasData[[ch]]
        count = count+1
      } # maxRep

    } # channel
  } # if(is.list(plotPlateArgs))



  ## ----------------------------------------------------------
  ## "Channel 2 vs Channel 1" plot (if nrChannels==2)
  if (nrChannel==2) {	
    ## correct the color code for the 2-channel scatter plot
    ## For the original configuration plate corrected by the screen log information:
    wellCount <- data.frame(matrix(NA, ncol = maxRep, nrow = 2))
    names(wellCount) = sprintf("Replicate %d", 1:maxRep)
    mtt <- vector("list", length = maxRep)

    if (isTwoWay) 
      ctrls <- unique(c(unlist(pact), unlist(pinh), unlist(pneg))) 
    else
      ctrls <- unique(c(unlist(ppos), unlist(pneg)))

    iPNAI <- which(names(wellTypeColor) %in% c("pos", "neg", "act", "inh"))

    for (r in 1:maxRep) {
      mtt[[r]] = mt
      mtrep = apply(finalWellAnno[,,r,, drop=FALSE], 4, match, names(wellTypeColor))

      ## set the controls in any of the channels as "controls":
      mtrep[ctrls,] [which(is.na(mtrep[ctrls,]) | mtrep[ctrls,] %in% iPNAI)] <- iwells[["controls"]]

      aa = apply(finalWellAnno[,,r,, drop=FALSE], 4, function(u) sum(u=="flagged"))
      aa = order(aa, decreasing=TRUE)
      nrWellTypes = sapply(seq(along=wellTypeColor), function(i) sum(mtrep[,aa[1]]==i, na.rm=TRUE))

      wellCount[1,r] = paste(sprintf("flagged: %d", nrWellTypes[iwells[["flagged"]]]), collapse=", ")
      wellCount[2, r] = paste(sprintf("<FONT COLOR=\"%s\">%s: %d</FONT>", wellTypeColor[-c(iwells[["flagged"]], iPNAI)], names(wellTypeColor)[-c(iwells[["flagged"]], iPNAI)], nrWellTypes[-c(iwells[["flagged"]], iPNAI)]), collapse=", ")
      ## so "flagged" or "empty" always wins over "controls" or "sample"
      mtt[[r]][is.na(mtt[[r]])] <- apply(mtrep[is.na(mtt[[r]]),, drop=FALSE], 1, max) 
      ## so "controls" always win over "pos" or "neg" or "sample" or "act" or "inh"
      mtt[[r]][!is.na(mtt[[r]])] <- apply(mtrep[!is.na(mtt[[r]]),, drop=FALSE], 1, max) 

    }## for r

    plotTable$"Channel 2 vs Channel 1" = ""

    ## plot title
    plotTable[3, 4] <- "<H5 align=center><FONT color=#494F8A>SCATTERPLOT BETWEEN CHANNELS</FONT></H5>\n"
    for (r in 1:maxRep) {

      if ( (r %in% whHasData[[1]]) & (r %in% whHasData[[2]]) ) {
        ## color legend:
        wellLeg = paste(sprintf("<CENTER>%s</CENTER><BR>\n", wellCount[1,r]),
          sprintf("<CENTER><em>Color legend: </em> %s</CENTER><BR>\n", wellCount[2,r]), collapse="")

        ## scatterplot between channels
        makePlot(file.path(basePath, subPath), con=con,
                 name=sprintf("scp_Rep%d", r), w=plsiz, h=plsiz, fun = function() {
                   par(mai=c(0.5,0.5,0.1,0.1))
                   ylim=c(min(platedat, na.rm=TRUE), max(platedat, na.rm=TRUE))
                   plot(platedat[,,r,1], platedat[,,r,2], pch=16, cex=0.5,
                        ylim=ylim, xlab="Channel 1", ylab="Channel 2", col=wellTypeColor[mtt[[r]]])
                   abline(a=0, b=1, col="lightblue")
                 }, print=FALSE)
        plotTable[r+3, 4] = sprintf("<CENTER>%s</CENTER><CENTER><A HREF=\"%s\"><IMG SRC=\"%s\"/></A></CENTER>\n", wellLeg, sprintf("scp_Rep%d.pdf", r), sprintf("scp_Rep%d.png", r)) 
      } else {
        plotTable[r+3, 4] = sprintf("<CENTER>Replicate %d is missing in one of the channels: scatterplot omitted</CENTER>\n", r)
      }## if r
    }## for r
  }## if nrChannel

  plotTable[is.na(plotTable)] = ""

  writeHTMLtable4plots(plotTable, con=con)
  writetail(con)
  return(list(url=fn, qmsummary=qmsummary)) 
}## function
