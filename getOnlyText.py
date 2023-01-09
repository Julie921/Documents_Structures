import sys
import re

def get_only_text_from_conll(conllu_file):
    """
    Cette fonction prend en entrée le nom d'un fichier au format CoNLL (`file`) et renvoie une chaîne de caractères contenant le texte du fichier, en ne conservant que les formes des mots et en sautant une ligne après chaque phrase.
    """
    # On ouvre le fichier en lecture et on lit toutes les lignes
    with open(conllu_file, "r", encoding="utf-8") as f:
        lines = f.readlines()
        
    texte = ""
        
    # Pour chaque ligne du fichier
    for line in lines:
        # Si la ligne ne contient pas uniquement un retour à la ligne
        if line[0] != "\n":
            # On récupère la forme du mot (2ème colonne du fichier CoNLL) et on ajoute un espace
            texte += line.split('\t')[1] + " "
        # Si la ligne ne contient qu'un retour à la ligne, on ajoute un retour à la ligne
        else:
            texte+="\n"
            
    # On renvoie le texte final
    return texte

def clean_texte_from_text_grid(texte: str) -> str:
    """
    Nettoye un texte en enlevant les guillemets, crochets, barres obliques, éléments comme <laugh> et les lignes vides. Le texte est également converti en minuscules.
    
    Params:
    - texte (str): Le texte à nettoyer.
    
    Returns:
    - str: Le texte nettoyé.
    """
    clean_texte = texte.replace('"', "").replace("[", "").replace("]", "").replace("/", "") # on enlève les guillemets, les crochets et les barres obliques
    clean_texte = re.sub(r"\<.+\>", "", clean_texte).replace("\n \n", "\n") # on enlève les <laugh> et les lignes vides qui ont pu apparaître à cause de ça
    return clean_texte.lower()
            
def get_only_text_from_textgrid(textgrid_file):
    texte = ""
    
    with open(textgrid_file, "r", encoding="utf-8") as tg:
        line = tg.readline()
        
        while line:
            if line.strip().startswith('text = "') and line.strip() != 'text = ""':
                texte += line.lstrip('texte = ') # on enlève le "texte = " au début de la ligne
            line = tg.readline()      
    
    return clean_texte_from_text_grid(texte)
    
    
def get_only_text(file):
    type = file.split(".")[-1] # l'extension du fichier
    
    if type == "TextGrid":
        return get_only_text_from_textgrid(file)
        
    elif type == "conllu":
        return get_only_text_from_conll(file)

if __name__=="__main__":
    
    my_file = sys.argv[1]
    my_text = get_only_text(my_file)
    
    #écriture du résultat dans un fichier texte
    with open(f"{my_file.split('/')[-1]}_only_text.txt", "w", encoding="utf-8") as f:
        f.write(my_text)       