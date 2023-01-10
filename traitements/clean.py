import re

def clean_token(token: str) -> str:
    """Nettoyage des tokens de tweet.
    
    Remplacements effectués : 
    
    - anonymisation : "@user"
    - "COL" -> ":"
    - liens -> "@link"
    - numéros -> "!digits
    
    Args:
        token (str): Le token à nettoyer.
    
    Returns:
        str: Le token nettoyé.
    """
    if re.match("@\w", token): # anonymisation des utilisateurs
        return "@user"
    elif re.match("COL", token):
        return ":"
    elif token.startswith("httpCOL//"): # on enlève les liens
        return "@link"
    elif re.match("\d", token): # on remplace les chiffres
        return "!digits"
    else: # on renvoie le token non modifié 
        return token

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
