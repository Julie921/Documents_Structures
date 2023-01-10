import csv
import re
import sys
from typing import List
from misc import safe_list_get

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


def convertConll(conll_file:str, converter:dict)->List[str]:
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
    
    with open(f"clustered_{conll_file.split('/')[-1]}", "w", encoding="utf-8") as output: # pour l'écriture du résultat
    
        with open(conll_file, "r") as input_conll: # lecture du conll
            l = input_conll.readline()
    
            while l: # tant qu'il reste des lignes
                source = safe_list_get(l.split("\t"), 1, False) # forme du token
                l_list = l.split("\t")
    
                if source in converter.keys(): # si la forme se trouve dnas le dico de conversion
                    l_list[1] = converter[source] # on remplace la forme du token par le nom du cluster
                    l_list[-1] = f"clustered = {source}\n" # on conserve la forme dans la colonne "misc"
    
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
    file = sys.argv[1] # le conll
    converter = makeDictFromTSV(tsv_file)
    convertConll(file, converter)

if __name__ == "__main__":
    
    main()