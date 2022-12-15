import sys

def get_only_text(file):
    
    with open(file, "r", encoding="utf-8") as f:
        lines = f.readlines()
        
    texte = ""
        
    for line in lines:
        if line[0] != "\n":
            texte += line.split('\t')[1] + " " #on ne récupère que la forme dans le conll et on ajoute un espace
        else:
            texte+="\n" #on saute une ligne après chaque phrase
            
    return texte
            
        
if __name__=="__main__":
    
    conll = sys.argv[1]
    
    texte = get_only_text(conll)
    
    #écriture du résultat dans un fichier texte
    with open(f"{conll.split('/')[-1]}_only_text.txt", "w", encoding="utf-8") as f:
        f.write(texte)       