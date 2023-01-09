import csv
import re
import sys


def safe_list_get(l, idx, default):
    try:
        return l[idx]
    except IndexError:
        return default


def makeDictFromCsv(csv_file):
    with open(csv_file, "r", encoding='utf8') as csvfile:
        dictCSV = csv.reader(csvfile, delimiter=',')
    converter = {row['unCluster']: row['cluster'] for row in dictCSV}
    return converter


def convertConll(conll_file, converter):
    file_as_list = []
    with open(f"clustered_{conll_file}", "w", encoding='utf8') as output:
        with open(conll_file, "r") as input_conll:
            l = input_conll.readline()
            while l:
                source = safe_list_get(l.split("\t"), 1, False)
                if source in converter.keys():
                    l_list = l.split("\t")
                    l_list[1] = converter[source]
                    l_list[-1] = f"clustered = {source}"

                output.write(l)
                file_as_list.append(l)
                l = input_conll.readline()
    return file_as_list


if __name__ == "__main__":
    csv_file = sys.argv[2]
    file = sys.argv[1]
    converter = makeDictFromCsv(csv_file)
    convertConll(file, converter)
