from sklearn.metrics import classification_report, multilabel_confusion_matrix
from traitements.conll_vs_spacy_pos import lire_conll as read
from traitements.conll_vs_spacy_pos import get_lexique, process_tokenized_corpus
from traitements.clean import clean_conll
from traitements import tagset_converter, cluster_converter
from traitements.misc import safe_list_get
import itertools
from eval import get_pos_list
from tools.sparknlp.loadSparkNLPmodel import main as sparknlp_test
from tools.sparknlp.trainSparkNLP import main as sparknlp_train
exec(open("tools/stanfordnlp/stanza-train/stanza-main/test_stanzaiy.py").read()) # récuperation des fonction test_my_model() et train_model() pour stanza
import spacy
import os
import glob as glb
import sys

def convert_to_UD(folder, conll_file):
    """
    fonction convertissant les pos d'un fichier conll de Penn vers UD
    prend en entrée un dossier, et le nom d'un fichier
    """
    converter = tagset_converter.makeDictFromCsv("./traitements/PenPos2UD.csv")
    tagset_converter.convertConll(folder, conll_file, 4, converter)
    return "prout"

def clean(folder,interFolder, conll_file,clustered=False):
    """
    fonction nettoyant les corpus de manière à uniformiser les données
    elle prend en entrée un dossier, un dossier intermédiaire et un fichier
    """
    clean_conll(folder, interFolder, conll_file)
    return "prout"

def cluster(folder, conll_file, fuzzy = False):
    """
    fonction clusterisant les corpus de manière à améliorer les résultats
    elle prend en entrée un dossier, un dossier intermédiaire et un fichier
    """
    if fuzzy:
        fuzzy=cluster_converter.makeDictFromTSV("./clustering/brown_hierarchy/fuzzy_matches.tsv", fuzzy=True)
    converter = cluster_converter.makeDictFromTSV("./clustering/brown_hierarchy/50mpaths2.txt")
    cluster_converter.convertConll(folder, conll_file, converter, fuzzy)
    return "prout"

def pretraitements(thing, clustered):
    """
    fonction de prétraitements
    elle prend en entrée le type de corpus (train, dev, test) et une variable controle permettant de savoir si les fichiers doivent etre clusterisés
    elle écrit les fichiers dans l'arborescence
    """

    thing_list = thing.split("/")
    folder, interFolder, thing = thing_list #séparation du chemin pour faciliter les manipulations des fichiers
    thing = thing.split(".")[0]
    thingy = thing

    if clustered:
        if clustered!="fuzzy":
            clean(folder, interFolder, thing)
            cluster(folder, f"clean_{thing}")
            convert_to_UD(folder, f"clustered_clean_{thing}")
            final_thing = f"{folder}/converted_clustered_clean_{thing}"
        else:
            clean(folder, interFolder, thing, clustered)
            cluster(folder, f"clean_{thing}", clustered)
            convert_to_UD(folder, f"clustered_fuzzy_clean_{thing}")
            final_thing = f"{folder}/converted_clustered_fuzzy_clean_{thing}"

    else:
        clean(folder, interFolder, thing)
        convert_to_UD(folder, f"clean_{thing}")
        final_thing = f"{folder}/converted_clean_{thing}"

    return final_thing, thingy

def make_ark_file(y_true):
    """
    création d'un fichier d'entrée formaté pour arknlp
    """
    with open("test_for_ark.conllu", "w") as test:
        for phrase in y_true:
            for tok, _ in phrase:
                if tok == "!digits":
                    test.write("digits\n")
                else:
                    test.write(tok.strip("'’")+"\n")
            test.write("\n")
def eval_tool(y_true_file, clustered, tool):
    """
    fonction d'évaluation sur tous nos fichiers de test
    """
    global corpus

    y_true_file, name = pretraitements(y_true_file, clustered)

    y_true = read(y_true_file)

    if tool == "spacy":
        nlp = spacy.load("./spacy_output/model-best/")
        vocab = get_lexique(y_true_file)
        y_pred, y_true = process_tokenized_corpus(nlp,y_true, vocab)
    elif tool == "sparknlp":
        y_true = [tok[1] for tok in list(itertools.chain.from_iterable(y_true))]
        y_pred = sparknlp_test("./sparknlp_output",y_true_file)
    elif tool == "stanfordnlp":
        y_true = [tok[1] for tok in list(itertools.chain.from_iterable(y_true))]
        y_pred = test_my_model(y_true_file)
    elif tool == "arknlp":
        make_ark_file(y_true)
        y_true = [tok[1] for tok in list(itertools.chain.from_iterable(y_true))]
        os.system(f"bash ./tools/outil_4/ark-tweet-nlp-0.3/ark-tweet-nlp-0.3.2/runTagger.sh --output-format conll test_for_ark.conllu > prout.conllu")
        conv = tagset_converter.makeDictFromCsv("./traitements/PenPos2UD.csv")
        tagset_converter.convertConll(".","prout",2,conv)
        y_pred = get_pos_list("converted_prout.conllu", "arknlp")


    folder_path = f"results/{tool}/trained_{corpus}"
    if clustered:
        if clustered != "fuzzy":
            folder_path += "/clustered/"
        else:
            folder_path += "/fuzzy_clustered/"
    else:
        folder_path += f"/unclustered/"

    os.makedirs(f"{folder_path}{name}", exist_ok=True)
    with open(f"{folder_path}{name}/classification_report.txt", "w") as clf_r:
        print(classification_report(y_true,y_pred), file=clf_r)
    with open(f"{folder_path}{name}/confusion_matrix.txt", "w") as clf_cm:
        print(multilabel_confusion_matrix(y_true,y_pred), file=clf_cm)
    return "prout"

def pipeline_train(folder,train, dev, clustered, tool):
    """
    fonction d'entrainements des outils
    """
    final_train, _ = pretraitements(train, clustered)
    final_dev, _ = pretraitements(dev, clustered)

    if tool == "spacy":
        os.system(f"python3 -m spacy convert {final_train}.conllu {folder}")

        os.system(f"python3 -m spacy convert {final_dev}.conllu {folder}")

        os.system(f"python3 -m spacy train ./tools/spacy/output/model-best/config.cfg --output ./spacy_output --paths.train {final_train}.spacy --paths.dev {final_dev}.spacy")
    elif tool == "sparknlp":
        sparknlp_train(train, dev, "./sparknlp_output/")
    elif tool == "arknlp":
        print("Cannot train tool arknlp")
    elif tool == "stanfordnlp":
        os.system(f"mv {final_train}.conllu ./tools/stanfordnlp/stanza-train/data/udbase/UD_English-TEST/en_test-ud-train.conllu")
        os.system(f"mv {final_dev}.conllu ./tools/stanfordnlp/stanza-train/data/udbase/UD_English-TEST/en_test-ud-dev.conllu")

        train_model()
    else:
        print(f"This script doesn't train with the tool {tool}, it can only take stanza, sparknlp or spacy")


    return "prout"



if __name__ == "__main__":
    tool = sys.argv[1]
    choice = sys.argv[2]
    clustered = safe_list_get(sys.argv, 4, False)
    corpus = sys.argv[3] # can be "only_aave" "only_english" or "concatenated"
    if choice == "train":
        folder = "./corpus"
        train = f"corpus/train/{corpus}.conllu"
        dev = "corpus/dev/aave_dev.conllu"
        pipeline_train(folder, train, dev, clustered, tool)
    elif choice == "test":
        folder = "corpus/test/*"
        files = glb.glob(folder)
        for file in files:
            eval_tool(f"{file}", clustered, tool)

    else:
        print("This isn't an option, this can only be used for training or testing : 'python pipeline.py test|train ...'")