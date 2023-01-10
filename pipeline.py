from sklearn.metrics import classification_report, multilabel_confusion_matrix
from traitements.conll_vs_spacy_pos import lire_conll as read
from traitements.conll_vs_spacy_pos import get_lexique, process_tokenized_corpus
from traitements.clean import clean_conll
from traitements import tagset_converter, cluster_converter
from traitements.misc import safe_list_get
import spacy
import os
import glob as glb
import sys

def convert_to_UD(folder, conll_file):
    converter = tagset_converter.makeDictFromCsv("./traitements/PenPos2UD.csv")
    tagset_converter.convertConll(folder, conll_file, 4, converter)
    return "prout"

def clean(folder,interFolder, conll_file):
    clean_conll(folder, interFolder, conll_file)
    return "prout"

def cluster(folder, conll_file):
    converter = cluster_converter.makeDictFromTSV("./clustering/brown_hierarchy/50mpaths2.txt")
    cluster_converter.convertConll(folder, conll_file, converter)
    return "prout"

def pretraitements(thing, clustered):

    global trashcan

    thing_list = thing.split("/")
    folder, interFolder, thing = thing_list
    thing = thing.split(".")[0]
    thingy = thing

    if clustered:
        clean(folder, interFolder, thing)
        cluster(folder, f"clean_{thing}")
        convert_to_UD(folder, f"clustered_clean_{thing}")
        final_thing = f"{folder}/converted_clustered_clean_{thing}"


    else:
        clean(folder, interFolder, thing)
        convert_to_UD(folder, f"clean_{thing}")
        final_thing = f"{folder}/converted_clean_{thing}"

    print(final_thing)

    return final_thing, thingy


def eval(y_true_file, clustered):

    global corpus

    nlp = spacy.load("./spacy_output/model-best/")
    y_true_file, name = pretraitements(y_true_file, clustered)
    vocab = get_lexique(y_true_file)
    y_true = read(y_true_file)
    y_pred, y_true = process_tokenized_corpus(nlp,y_true, vocab)
    folder_path = f"results/spacy/trained_{corpus}"
    if clustered:
        folder_path += "/clustered/"
    else:
        folder_path += f"/unclustered/"

    os.makedirs(f"{folder_path}{name}", exist_ok=True)
    with open(f"{folder_path}{name}/classification_report.txt", "w") as clf_r:
        print(len(y_pred))
        print(len(y_true))
        print(classification_report(y_true,y_pred), file=clf_r)
    with open(f"{folder_path}{name}/confusion_matrix.txt", "w") as clf_cm:
        print(multilabel_confusion_matrix(y_true,y_pred), file=clf_cm)
    return "prout"

def pipeline_train(folder,train, dev, clustered):

    global trashcan

    final_train, _ = pretraitements(train, clustered)
    final_dev, _ = pretraitements(dev, clustered)

    os.system(f"python3 -m spacy convert {final_train}.conllu {folder}")

    os.system(f"python3 -m spacy convert {final_dev}.conllu {folder}")

    os.system(f"python3 -m spacy train ./tools/spacy/output/model-best/config.cfg --output ./spacy_output --paths.train {final_train}.spacy --paths.dev {final_dev}.spacy")



    return "prout"



if __name__ == "__main__":
    choice = sys.argv[1]
    clustered = safe_list_get(sys.argv, 3, False)
    corpus = sys.argv[2] # can be "only_aave" "only_english" or "concatenated"
    if choice == "train":
        folder = "./corpus"
        train = f"corpus/train/{corpus}.conllu"
        dev = "corpus/dev/aave_dev.conllu"
        pipeline_train(folder, train, dev, clustered)
    elif choice == "test":
        folder = "corpus/test/*"
        files = glb.glob(folder)
        for file in files:
            eval(f"{file}", clustered)
    print("prout")