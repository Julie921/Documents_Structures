import sys
import itertools

style = open(sys.argv[1] + '/style.css').read()

def get_word_rows():
    """Lit les lignes d'entrée standard et renvoie un générateur qui produit des tuples contenant le chemin, le mot et le nombre d'occurrences de chaque mot pour chaque ligne.
    
    Les lignes ressemblent à : `1110010011110	received	1`
    """
    for line in sys.stdin:
        path, word, count = line.split('\t')
        count = int(count)
        yield path, word, count

def get_cluster_rows():
    """Lit les tuples produits (path, word, count) par la fonction `get_word_rows()` et renvoie un générateur qui produit des tuples contenant le chemin, le nombre de mots distincts, les 50 premiers mots classés par ordre de fréquence décroissante et tous les mots classés par ordre de fréquence décroissante pour chaque groupe de tuples partageant le même chemin."""
    for path, rows in itertools.groupby(get_word_rows(), key=lambda x: x[0]): # groupe les tuples (rank, mot, compte) selon le chemin
        wordcounts = [(w,c) for _, w, c in rows] # liste de tuples (mot, compte)
        wordcounts = sorted(wordcounts, key=lambda x: x[1], reverse=True) # on trie selon le compte décroissant

        yield path, len(wordcounts), wordcounts[:50], wordcounts

def htmlescape(s):
    """Fonction pour remplacer les entités en HTML"""
    return s.replace('&','&amp;').replace('<','&lt;').replace('>','&gt;')

def wc_table(wordcounts, tdword=''):
    """Cette fonction prend en entrée une liste de tuples contenant des mots et leur fréquence d'apparition (wordcounts) et renvoie une chaîne de caractères représentant un tableau HTML avec ces données. Le paramètre optionnel tdword peut être utilisé pour spécifier une classe CSS à appliquer aux cellules contenant les mots. La fonction utilise la fonction htmlescape pour échapper les caractères HTML dans les mots afin qu'ils puissent être affichés en toute sécurité dans le tableau HTML."""
    r = ['<table>'] # début du tableau
    for i, (w, c) in enumerate(wordcounts):
        r.append(f'<tr><td>{i+1} <td class="{tdword}">{htmlescape(w)} <td class=tdcount>{c:,}')
    r.append('</table>') # fin de la table
    return '\n'.join(r)

def top(wc, th):
    """Cette fonction prend en entrée une liste de tuples contenant des mots et leur fréquence d'apparition (wc) et un seuil (th) sous forme de nombre flottant compris entre 0 et 1. Elle renvoie une liste de tuples avec les mots de wc dont la fréquence d'apparition est supérieure au seuil spécifié, exprimé en pourcentage de la fréquence d'apparition la plus élevée. Par exemple, si le seuil est fixé à 0,5 et que la fréquence d'apparition la plus élevée est de 100, la fonction renverra tous les mots dont la fréquence d'apparition est supérieure à 50."""
    cutoff = int(wc[0][1] * th)
    res = []
    for (w, c) in wc:
        if c > cutoff: 
            res.append((w,c))
    return res

for path, nwords, wordcounts, allwc in get_cluster_rows():
    # wc1 = ' '.join("<span class=w>{w}</span>&thinsp;<span class=c>[{c}]</span>".format(
    #     w=htmlescape(w), c=c) for w,c in wordcounts)
    #wc1 = ' '.join("<span class=w>{w}</span>".format(w=htmlescape(w)) for w,c in top(wordcounts, 0.01))
    wc1 = ' '.join(f"<span class=w>{htmlescape(w)}</span>" for (w, _) in top(wordcounts, 0.01))
    
    print(f"""
    <tr>
    <td class=path>^<a target=_blank href="paths/{path}.html">{path}</a> <span class=count>({nwords})</span>
    <td class=words>{wc1}
    </tr>
    """)

    with open(sys.argv[2] + '/paths/{path}.html'.format(**locals()),'w') as f:
        print("""<style>{style}</style>""".format(**locals()), file=f)
        print("""<meta http-equiv="Content-Type" content="text/html;charset=UTF-8">""", file=f)
        print("<a href=../cluster_viewer.html>back to cluster viewer</a>", file=f)
        print("<h1>cluster path {path}</h1>".format(path=path), file=f)
        
        print("{n:,} words, {t:,} tokens".format(n=nwords, t=sum(c for w,c in allwc)), file=f)
        print("<a href='#freq'>freq</a> <a href='#alpha'>alpha</a> <a href='#suffix'>suffix</a>", file=f)

        print("<a name=freq><h2>Words in frequency order</h2></a>", file=f)
        #allwc.sort(key=lambda w,c: (-c,w)) # ne marche pas
        allwc = sorted(allwc, key=lambda x: (-x[1], x[0]))
        print(wc_table(allwc), file=f)
        # wc1 = ' '.join("<span class=w>{w}</span>&nbsp;<span class=c>({c})</span>".format(
        #     w=htmlescape(w), c=c) for w,c in allwc)
        # print>>f, wc1

        print("<a name=alpha><h2>Words in alphabetical order</h2></a>", file=f)
        #allwc.sort(key=lambda w,c: (w,-c))
        allwc = sorted(allwc, key=lambda x: (-x[1], x[0]))
        print(wc_table(allwc), file=f)

        print("<a name=suffix><h2>Words in suffix order</h2></a>", file=f)
        #allwc.sort(key=lambda w,c: (list(reversed(w)),-c))
        print(wc_table(allwc, tdword='suffixsort'), file=f)
        # wc1 = ' '.join("<span class=w>{w}</span>&nbsp;<span class=c>({c})</span>".format(
        #     w=htmlescape(w), c=c) for w,c in allwc)
        # print>>f, wc1