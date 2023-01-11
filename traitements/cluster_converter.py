import csv
import re
import sys
from typing import List, Any
#from misc import safe_list_get

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

def makeDictFromTSV(tsv_file: str) -> dict:
    """
    Cette fonction prend un fichier TSV en entrée et crée un dictionnaire en lisant le fichier.
    Chaque ligne du fichier est censée contenir 2 valeurs (minimum) séparées par une tabulation, avec la première valeur comme clé et la deuxième valeur comme valeur dans le dictionnaire.
    Le dictionnaire créé est retourné.
    
    Exemple du fichier d'entrée :
    
    ```plain
    0001111    you've    100
    0001111    ivee     106
    0001111    i'd've   114
    ```
    
    Exemple de dictionnaire de sortie :
    
    ```python
    {"you've": "0001111", 
    "ivee": "0001111", 
    "i'd've": "0001111"}
    ```
    
    Args: 
        tsv_file (str): chaîne représentant le chemin d'accès au fichier tsv.
    
    Returns: 
        dict: un dictionnaire créé à partir du fichier tsv
    """
    converter=dict()
    with open(tsv_file, "r", encoding='utf8') as file:
        dictTSV = csv.reader(file, delimiter='\t')
        for row in dictTSV:
            converter[row[1]] = row[0]
    return converter


def remove_repeats(word):
    """Enlève les caractères répétés pour pouvoir trouver un mot dans
    le dictionnaire des fuzzy matches si jamais il ne se trouve pas
    dans celui des clusters
    
    Args:
        word (str): le mot qui ne se trouve pas dans les clusters (par exemple: "woooooowwwww")
    
    Returns:
        str: le mot où on a enlevé tous les caractères répétés (ex: "wow")
    """
    return re.sub(r'([^\w\s])\1+', r'\1', word)

def convertConll(folder,conll_file:str, converter:dict, fuzzyMatches: dict|bool = False)->List[str]:
    """Cette fonction prend en entrée un fichier CoNLL déjà nettoyé
    et un dictionnaire de correspondances de remplacement (forme -> nom_cluster). 
    Elle sert à exploiter les clusters créés avec l'algorithme de Brown.
    
    Elle écrit un nouveau CoNLL dans lequel la forme des tokens a été
    remplacée par sa valeur dans le dictionnaire de correspondances.  
    Elle conserve la forme originale du token dans la colonne `misc` du CoNLL.
    
    Le nom du nouveau CoNLL porte la mention `clustered_`. En plus de l'écriture
    du fichier, le contenu du fichier est retourné sous forme d'une liste de strings si jamais on a besoin de s'en servir.

    Args:
        conll_file (str): le fichier CoNLL à convertir
        converter (dict): un dictionnaire de correspondance

    Returns:
        file_as_list (List[str]): le contenu du nouveau fichier CoNLL converti sous forme de liste de strings
    """
    
    file_as_list = [] # le fichier sous forme de liste si jamais on veut tester des trucs
    
    with open(f"{folder}/clustered_{conll_file}.conllu", "w", encoding="utf-8") as output: # pour l'écriture du résultat
    
        with open(f"{folder}/{conll_file}.conllu", "r") as input_conll: # lecture du conll
            l = input_conll.readline()
    
            while l: # tant qu'il reste des lignes
                source = safe_list_get(l.split("\t"), 1, False) # forme du token
                l_list = l.split("\t")
    
                if source in converter.keys(): # si la forme se trouve dnas le dico de conversion
                    l_list[1] = converter[source] # on remplace la forme du token par le nom du cluster
                    l_list[-1] = f"clustered = {source}\n" # on conserve la forme dans la colonne "misc"
                elif fuzzyMatches:
                    if remove_repeats(source) in fuzzyMatches.keys(): # s'il n'est pas dans les clusters, on le cherche dans les fuzzy matches
                        l_list[1] = converter[fuzzyMatches[source]] # on utilise le cluster du fuzzy match
                        l_list[-1] = f"clustered = {source}\n" # on conserve la forme originale dans la colonne "misc"
                l = "\t".join(l_list) # on reconstitue la ligne
                output.write(l)
                file_as_list.append(l) 
                l = input_conll.readline() # on passe à la ligne suivante 
    
    return file_as_list

def main():
    """Pour lancer le script : 
    
    ```bash
    python3 cluster_converter.py conll_file tsv_file
    ```
    """

    tsv_file = sys.argv[2] # fichier avec les correspondances
    conll_file = sys.argv[1] # le conll
    fuzzyMatches_file = sys.argv[2] # le tsv avec les fuzzy matches
    
    converter = makeDictFromTSV(tsv_file)
    fuzzyMatches = makeDictFromTSV(fuzzyMatches) # dictionnaire qui map pour chaque mot non-présnet dans les clusters le mot qui est le plus proche de lui dans les clusters
    convertConll(conll_file, converter)

if __name__ == "__main__":
    
    main()