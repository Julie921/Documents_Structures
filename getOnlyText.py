import sys

def get_only_text(file):
    """
    Cette fonction prend en entrée le nom d'un fichier au format CoNLL (`file`) et renvoie une chaîne de caractères contenant le texte du fichier, en ne conservant que les formes des mots et en sautant une ligne après chaque phrase.
    """
    # On ouvre le fichier en lecture et on lit toutes les lignes
    with open(file, "r", encoding="utf-8") as f:
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
            
        
if __name__=="__main__":
    
    my_conll = sys.argv[1]
    my_text = get_only_text(my_conll)
    
    #écriture du résultat dans un fichier texte
    with open(f"{my_conll.split('/')[-1]}_only_text.txt", "w", encoding="utf-8") as f:
        f.write(my_text)       