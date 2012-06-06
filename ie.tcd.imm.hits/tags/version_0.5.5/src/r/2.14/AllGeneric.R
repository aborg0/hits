## =================================================
## generic functions:
## =================================================
setGeneric("plate", def=function(object) standardGeneric("plate"))

setGeneric("well", def=function(object) standardGeneric("well"))

setGeneric("wellAnno", def=function(object) standardGeneric("wellAnno"))

setGeneric("pdim", def=function(object) standardGeneric("pdim"))

setGeneric("position", def=function(object) standardGeneric("position"))

setGeneric("annotate", def=function(object, geneIDFile, path=dirname(geneIDFile))
           standardGeneric("annotate"))

setGeneric("geneAnno", def=function(object) standardGeneric("geneAnno"))

setGeneric("Data", def=function(object) standardGeneric("Data"))

setGeneric("Data<-", def=function(object, value) standardGeneric("Data<-"))

setGeneric("state", def=function(object) standardGeneric("state"))

setGeneric("plateList", def=function(object) standardGeneric("plateList"))

setGeneric("plateConf", def=function(object) standardGeneric("plateConf"))

setGeneric("screenLog", def=function(object) standardGeneric("screenLog"))

setGeneric("screenDesc", def=function(object) standardGeneric("screenDesc"))

setGeneric("intensityFiles", def=function(object) standardGeneric("intensityFiles"))

setGeneric("configure", def=function(object, descripFile, confFile, logFile, path, ...)
           standardGeneric("configure"))

setGeneric("writeTab", def=function(object, file=paste(name(object), "txt", sep="."))
           standardGeneric("writeTab"))

setGeneric("name", def=function(object) standardGeneric("name"))

setGeneric("name<-", def=function(object, value) standardGeneric("name<-"))

setGeneric("plateEffects", def=function(object) standardGeneric("plateEffects"))

setGeneric("batch", def=function(object) standardGeneric("batch"))

setGeneric("batch<-", def=function(object, value) standardGeneric("batch<-"))

setGeneric("nbatch", def=function(object) standardGeneric("nbatch"))

setGeneric("compare2cellHTS", def=function(x,y) standardGeneric("compare2cellHTS"))

setGeneric("ROC", def=function(object, positives, negatives) standardGeneric("ROC"))

setGeneric("writeHtml",  function(x, ...) standardGeneric("writeHtml"))

setGeneric("channelNames<-", def=function(object, value) standardGeneric("channelNames<-"))

#if(!isGeneric("lines"))
#    setGeneric("lines", useAsDefault=lines)
