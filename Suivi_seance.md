# Suivi

## 10/11 

Delphine : 
- environnement virtuelle créer -> partager les info
- fonctionnement de Stanford - Stanza 
- préparation fichier train test (erreurs)


Aurélien : 
- script de conversion vers conllu. transférer les tags sur la version conllu.

Julie :
- résumé des articles 
- d'autres outils possibles
    1. ARK Tagger : http://www.ark.cs.cmu.edu/TweetNLP 
        cf : Learning a POS tagger for AAVE-like language, 2016
    2. POS TAG : https://github.com/Oneplus/Tweebank and https://github.com/Oneplus/twpipe.
        cf : Parsing Tweets into Universal Dependencies, 2018
    3. POS TAG avec ambiguité : https://bitbucket.org/soegaard/aave-pos16/src/master/
        cf : Improved Part-of-Speech Tagging for Online Conversational Text with Word Clusters, 2013
        
        
## 8/12 : Plan d’attaque

### Choix de la tâche :
PosTAGGING
Analyse en dépendance (?)

### Liste des outils :
- 1er outil : spacy
- 2e outil : stanza
- 3e outil : https://bitbucket.org/soegaard/aave-pos16/src/master/
	- Jørgensen, Anna, Dirk Hovy, et Anders Søgaard. « Learning a POS tagger for AAVE-like language ». In Proceedings of the 2016 Conference of the North American Chapter of the Association for Computational Linguistics: Human Language Technologies, 1115‑20. San Diego, California: Association for Computational Linguistics, 2016. https://doi.org/10.18653/v1/N16-1130.
4e outil : ARK Tagger https://github.com/brendano/ark-tweet-nlp/tree/master/data/twpos-data-v0.3
	- Blodgett, Su Lin, Johnny Wei, et Brendan O’Connor. « Twitter Universal Dependency Parsing for African-American and Mainstream American English ». In Proceedings of the 56th Annual Meeting of the Association for Computational Linguistics (Volume 1: Long Papers), 1415‑25. Melbourne, Australia: Association for Computational Linguistics, 2018. https://doi.org/10.18653/v1/P18-1131.
5e outil : pipeline ici : Our dataset and system are publicly available at https://github.com/Oneplus/Tweebank and https://github.com/Oneplus/twpipe.
	- Liu, Yijia, Yi Zhu, Wanxiang Che, Bing Qin, Nathan Schneider, et Noah A. Smith. « Parsing Tweets into Universal Dependencies ». arXiv, 22 avril 2018. http://arxiv.org/abs/1804.08228.

### Liste des données :
- Corpus Twitter
    - Twitter AAE : Corpus de tweets AAVE et WH distingués
- Corpus Sous-Titres : 
    - Sous-titres de la série The Wire
    - A Test Suite for Evaluating POS Taggers across Varieties of English

### Liste des expériences :
1. Entraînement sur Anglais (depuis les modèles) -> Test sur Anglais
2. Entraînement sur Anglais (depuis nos corpus) -> Test sur Anglais
3. Entraînement sur Anglais (depuis les modèles) -> Test sur AAVE
4. Entraînement sur Anglais (depuis nos corpus) -> Test sur AAVE
5. Entraînement sur AAVE -> Test sur AAVE 

### Liste des expés potentielles.
Entraînement sur AAVE -> Test sur Anglais (?)
Dépendance ?

### Les corpus 

1. TwitterAAE : by Blodgett et al. (2016). http://slanglab.cs.umass.edu/TwitterAAE/. A new dataset of 500 tweets, with a total of 5,951 non-punctuation edges, 250 of which are in AAE, within the Universal Dependencies 2.0 framework.
	- Blodgett, Su Lin, Lisa Green, et Brendan O’Connor. « Demographic Dialectal Variation in Social Media: A Case Study of African-American English ». In Proceedings of the 2016 Conference on Empirical Methods in Natural Language Processing, 1119‑30. Austin, Texas: Association for Computational Linguistics, 2016. https://doi.org/10.18653/v1/D16-1120.
	- Dacon, Jamell. « Towards a Deep Multi-layered Dialectal Language Analysis: A Case Study of African-American English ». arXiv, 2 juin 2022. http://arxiv.org/abs/2206.08978.
	- Blodgett, Su Lin, Johnny Wei, et Brendan O’Connor. « Twitter Universal Dependency Parsing for African-American and Mainstream American English ». In Proceedings of the 56th Annual Meeting of the Association for Computational Linguistics (Volume 1: Long Papers), 1415‑25. Melbourne, Australia: Association for Computational Linguistics, 2018. https://doi.org/10.18653/v1/P18-1131.
 
2. TWEEBANK V2 : is built on the original data of TWEEBANK V1 (840 unique tweets, 639/201 for training/test), along with an additional 210 tweets sampled from the POS-tagged dataset of Gimpel et al. (2011) and 2,500 tweets sampled from the Twitter stream from February 2016 to July 2016. The latter data source consists of 147.4M English tweets after being filtered by the lang attribute in the tweet JSON and langid.py. Oneplus/Tweebank: A collection of English tweets annotated in Universal Dependencies. (github.com)
	- Liu, Yijia, Yi Zhu, Wanxiang Che, Bing Qin, Nathan Schneider, et Noah A. Smith. « Parsing Tweets into Universal Dependencies ». arXiv, 22 avril 2018. http://arxiv.org/abs/1804.08228.
 
3. 12 datasets for evaluating POS taggers across varieties of English (https://bitbucket.org/lowlands/). The suite includes three new datasets, sampled from lyrics from black American hip-hop artists, southeastern American Twitter, and the subtitles from the TV series The Wire. → twitter, subtitles and lyrics : three new English datasets from domains containing non-canonical language use reflecting aspects of African American Vernacular English (AAVE) - twitter AAVE  →The tweets for the AAVE tweets dataset were collected through the Twitter API using the Python package, TwitterSearch,8 between 2015-02-282015-03-03. => https://bitbucket.org/soegaard/aave-pos16/src/master/data/test/amb/ tout est ici.
- Jørgensen, Anna, et Anders Søgaard. « A Test Suite for Evaluating POS Taggers across Varieties of English ». In Proceedings of the 25th International Conference Companion on World Wide Web - WWW ’16 Companion, 615‑18. Montréal, Québec, Canada: ACM Press, 2016. https://doi.org/10.1145/2872518.2890559.
- Jørgensen, Anna, Dirk Hovy, et Anders Søgaard. « Learning a POS tagger for AAVE-like language ». In Proceedings of the 2016 Conference of the North American Chapter of the Association for Computational Linguistics: Human Language Technologies, 1115‑20. San Diego, California: Association for Computational Linguistics, 2016. https://doi.org/10.18653/v1/N16-1130.
 
4. Corpus of POS-annotated tweets released by CMU : https://github.com/brendano/ark-tweet-nlp/tree/master/data/twpos-data-v0.3 consisting of semi-randomly sampled US tweets. (https://bitbucket.org/soegaard/aave-pos16/src/master/data/README.txt)
- Jørgensen, Anna, Dirk Hovy, et Anders Søgaard. « Learning a POS tagger for AAVE-like language ». In Proceedings of the 2016 Conference of the North American Chapter of the Association for Computational Linguistics: Human Language Technologies, 1115‑20. San Diego, California: Association for Computational Linguistics, 2016. https://doi.org/10.18653/v1/N16-1130.



