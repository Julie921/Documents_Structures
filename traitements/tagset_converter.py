import csv
import re
import sys
#from misc import safe_list_get
from typing import List, Any


def makeDictFromCsv(csv_file):
    converter=dict()
    with open(csv_file, "r", encoding='utf8') as csvfile:
        dictCSV = csv.reader(csvfile, delimiter=';')
        for row in dictCSV:
            converter[row[1]] = row[0]
    return converter


def safe_list_get(l: List[Any], idx: int, default: Any) -> Any:
    """
    Cette fonction prend en entrée une liste, un indice et une valeur par défaut. Elle renvoie l'élément de la liste à l'indice spécifié, si l'indice est en dehors des limites de la liste, elle retourne la valeur par défaut.
    Cela permet d'éviter les erreurs d'indexation qui pourraient se produire lors de l'accès à un élément de la liste.

    Args:
    - l (List[Any]): une liste
    - idx (int): un indice
    - default: valeur par défaut

    Returns:
    - Any : l'élément de la liste à l'indice spécifié ou la valeur par défaut.
    """
    try:
        return l[idx]
    except IndexError:
        return default

def convertConll(folder,conll_file, col_n, converter):
    file_as_list = []
    conll_file_f = conll_file

    with open(f"{folder}/converted_{conll_file_f}.conllu", "w", encoding='utf8') as output:
        with open(f"{folder}/{conll_file}.conllu", "r") as input_conll:
            l = input_conll.readline()
            while l:
                l_list = l.split("\t")
                source = safe_list_get(l.split("\t"), col_n, False)
                if source in converter.keys():
                    l_list[col_n] = converter
                    output.write("\t".join(l_list))
                    file_as_list.append("\t".join(l_list))
                else:
                    output.write(l)
                    file_as_list.append(l)
                l = input_conll.readline()
    return file_as_list


if __name__ == "__main__":
    csv_file = sys.argv[2]
    file = sys.argv[1]
    if len(sys.arvg) <= 3:
        col_n = 4
    col_n = sys.argv[3] - 1
    converter = makeDictFromCsv(csv_file)
    convertConll(file, col_n, converter)
