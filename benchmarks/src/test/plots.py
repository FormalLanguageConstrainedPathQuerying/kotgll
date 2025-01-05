import os
import csv
from matplotlib import pyplot as plt
from pathlib import Path


def get_datasets_files(folder: str) -> dict[str, list[str]]:
    return {

        dataset_folder: [os.path.join(folder, dataset_folder, file) for file in os.listdir(os.path.join(folder, dataset_folder)) if file.endswith(".csv")]
        for dataset_folder in os.listdir(folder) if os.path.isdir(os.path.join(folder, dataset_folder))
    }


def read_file_data(file: str, column: int) -> list[float]:
    with open(file) as fin:
        rdr = csv.DictReader(filter(lambda row: row[0] != "%", fin))
        return [float(list(line.values())[column]) for line in rdr]


def read_data(folder: str, column: int) -> dict[str, dict[str, list[float]]]:
    datasets_files = get_datasets_files(folder)
    return {dataset: {Path(file).stem: read_file_data(file, column) for file in files} for dataset, files in datasets_files.items()}


def draw_fig(folder: str, column: int, filename: str):
    dict = read_data(folder, column)
    tools = set(tool for tools_values in dict.values() for tool in tools_values.keys())
    all_datasets = {tool: [value for _, tools_values in dict.items() for value in tools_values.get(tool, [])] for tool in tools}

    result = {"All projects": all_datasets}
    result.update(dict)

    fig, axs = plt.subplots(len(result))
    for (dataset, values), ax in zip(result.items(), axs):
        ax.boxplot(values.values(), vert=False, widths=0.6)
        ax.set_yticklabels(values.keys())
        ax.set_title(dataset, loc='right')
    fig.tight_layout()
    fig.savefig(filename)


draw_fig("results", 1, "time.png")
# draw_fig(2, "memory.png")
