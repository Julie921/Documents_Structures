import re
from typing import List
from misc import safe_list_get
import sys


####################################################################################


def clean_token(token: str) -> str:
    """Nettoyage des tokens de tweet.
    
    Remplacements effectués : 
    
    - anonymisation : "@user"
    - "COL" -> ":"
    - liens -> "@link"
    - numéros -> "!digits
    
    Tout est mis en minuscules.
    
    Args:
        token (str): Le token à nettoyer.
    
    Returns:
        str: Le token nettoyé et mis en minuscules
    """
    if re.match("@\w", token): # anonymisation des utilisateurs
        return "@user"
    elif re.match(r"URL[0-9]+", token): # on enlève les liens qui étaient sous la forme "URL1234", etc.
        return re.sub(r"URL[0-9]+", "@link", token)
    elif token.startswith("httpCOL//") or token.startswith("httpsCOL//"): # on enlève les liens
        return "@link"
    elif re.search("COL", token):
        return re.sub("COL", ":", token).lower()
    elif re.match("\d", token): # on remplace les chiffres
        return "!digits"
    else: # on renvoie le token non modifié 
        return token.lower()
    

def clean_texte_from_text_grid(texte: str) -> str:
    """
    Nettoye le texte provenant d'un TextGrid en mettant tout en minuscules et en enlevant : 
    
    - les crochets (qui indiquent que deux locuteurs parlent en même temps) : "So [I went-]"
    - les barres obliques (qui indiquent les mots mal prononcés ou redacted)
    - les guillemets
    - les chevrons et leur contenu (qui indiquent des sons non-linguistique) : <laugh>
    - les parenthèses et leur contenu (indiquent des notes de l'annotateur) : (laughing)
    
    Params:
    - texte (str): Le texte issu du TextGrid à nettoyer.
    
    Returns:
    - str: Le texte nettoyé.
    """
    clean_texte = texte.replace('"', "").replace("[", "").replace("]", "").replace("/", "") # on enlève les guillemets, les crochets et les barres obliques
    clean_texte = re.sub(r"\<.+\>", "", clean_texte).replace("\n \n", "\n") # on enlève les <laugh> et les lignes vides qui ont pu apparaître à cause de ça
    clean_texte = re.sub(r"\(.+\)", "", clean_texte).replace("\n \n", "\n") # on enlève (laughing)
    return clean_texte.lower() # on met tout en minuscules

def clean_conll(conll_file: str) -> List[str]:
    """Nettoie un fichier CoNLL donné en entrée.
    
    En plus d'écriture le CoNLL nettoyé dans un nouveau fichier, la fonction renvoie le fichier nettoyé 
    sous la forme d'une liste de chaînes de caractères pour pouvoir l'exploiter si besoin.    

    Args:
        conll_file (str): Le chemin d'accès au fichier CoNLL qui doit être nettoyé.

    Returns:
        clean_conll_as_list (List[str]): Une liste de chaînes de caractères, où chaque chaîne est une ligne du fichier CoNLL nettoyé.
    """

    clean_file_as_list = [] # le fichier propre sous la forme d'une liste de strings

    with open(f"clean_{conll_file.split('/')[-1]}", "w", encoding="utf-8") as output:
        
        with open(conll_file, "r", encoding="utf-8") as input:
            l = input.readline()
            
            while l:
                
                if safe_list_get(l.split("\t"), 1, False): # si on se trouve bien sur une ligne avec les colonnes du conll
                    l_list = l.split("\t")
                    l_list[1] = clean_token(l_list[1]) # nettoyage du token
                    l_list[4] = "_" # on enlève ce qui se trouve dans la cinquime colonne
                    l_clean = "\t".join(l_list)
                    output.write(l_clean) # écriture de la ligne propre
                    clean_file_as_list.append(l_clean) # ajout de la ligne dans notre liste de lignes au cas où
                
                else: # il ne s'agit pas d'une ligne avec un token
                    clean_file_as_list.append(l)
                    output.write(l)
                    
                l = input.readline()
                    
    return clean_file_as_list

def main():
    """
    Exemple de comment lancer le script : 
    
    ```bash
    python3 clean.py file.conllu
    ```
    """
    
    conll_file = sys.argv[1]
    
    clean_conll(conll_file)

####################################################################################

if __name__=="__main__":
    
    main()