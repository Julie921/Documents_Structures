from typing import List, Any

def safe_list_get(l:List[Any], idx:int, default:Any)->Any:
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