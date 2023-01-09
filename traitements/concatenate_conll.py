import sys
import random


def convertConll(conll_file):
    file_as_list = []
    with open(conll_file, "r") as input_conll:
        tmp = []
        l = input_conll.readline()
        while l:
            while l != "\n":
                tmp.append(l)
                l = input_conll.readline()
            tmp.append(l)
            file_as_list.append(tmp)
            tmp = []
            l = input_conll.readline()
    return file_as_list


def shuffle_merge(conll1, conll2):
    merged_conll = conll1 + conll2
    print(conll1[:10])
    print(conll2[:10])
    random.shuffle(merged_conll)
    return merged_conll


def rewrite_conll_from_list(merged_conll, filename):
    with open(filename, "w") as cool:
        for sentence in merged_conll:
            for line in sentence:
                cool.write(line)
    return "prout"


def shuffle_merge_two_conll(conll1, conll2, filename):
    conll1_list = convertConll(conll1)
    conll2_list = convertConll(conll2)
    merged_conll = shuffle_merge(conll1_list, conll2_list)
    print(rewrite_conll_from_list(merged_conll,filename))
    return "prout"


if __name__ == "__main__":
    file1 = sys.argv[1]
    file2 = sys.argv[2]
    filename = sys.argv[3]
    shuffle_merge_two_conll(file1, file2,filename)
