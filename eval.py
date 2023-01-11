import sys
from sklearn.metrics import classification_report

dico_conll = {'UPOS': 3, 'XPOS': 4, 'arknlp': 1}


def safe_list_get(l, idx, default):
    try:
        return l[idx]
    except IndexError:
        return default


def get_pos_list(file, pos_type):
    global dico_conll
    pos_list = []
    col_n = dico_conll[pos_type]
    with open(file, "r") as input_conll:
        l = input_conll.readline()
        while l:
            source = safe_list_get(l.split("\t"), col_n, False)
            if source:
                pos_list.append(source)
            l = input_conll.readline()
    return pos_list


if __name__ == "__main__":
    y_true = sys.argv[1]
    y_pred = sys.argv[2]
    print(classification_report(y_true, y_pred))
