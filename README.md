# ModelPolisher [![Build Status](https://travis-ci.org/SBRG/ModelPolisher.svg?branch=master)](https://travis-ci.org/SBRG/ModelPolisher)

<img src="doc/img/ModelPolisherIcon256.png" width="64"/>

ModelPolisher accesses the [BiGG Models knowledgebase](http://bigg.ucsd.edu) to annotate and autocomplete [SBML](http://sbml.org) models.
Thereby, the program mainly relies on [BiGG identifiers](https://github.com/SBRG/bigg_models/wiki/BiGG-Models-ID-Specification-and-Guidelines) for model components.
In addition, it fixes some obvious errors in the models.

An online version of this program is available at https://webservices.cs.uni-tuebingen.de: Just click on the program's icon and upload a file using the link on the left.

ModelPolisher is essentially a command-line based tool. You can run it locally using your own installation of BiGG Models database (see https://github.com/SBRG/bigg_models). A list of all available command-line options is printed when starting ModelPolisher with the option `-?`, i.e., by typing `java -jar ModelPolisher-VERSION.jar -?`, where "VERSION" needs to be replaced with the current release version of the program. If you run into trouble with larger files, especially _java.lang.OutOfMemoryError: GC overhead limit exceeded_, please use the jvm Xmx flag (e.g. `java -Xmx4G -jar ModelPolisher-VERSION.jar` ).

# How to cite ModelPolisher?

The online version of ModelPolisher is described in this article: http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0149263

The article http://nar.oxfordjournals.org/content/44/D1/D515 describes BiGG Models knowledge-base including ModelPolisher, http://nar.oxfordjournals.org/content/44/D1/D515
