
import os
import subprocess
import sys

cp_path = '/home/olga/gllgen/ucfs/simpleApp/build/install/simpleApp/lib/*'
files_dir = '/home/olga/gllgen/java7/junit/'

tool = sys.argv[1]
files_dir = sys.argv[2]
cp_path = sys.argv[3]

def run_tool_for_file(tool, file_path):
    def get_cmd(mem):
        return ['java', '-cp', cp_path, f'-Xmx{mem}m', 'org.ucfs.MainKt', tool, file_path]


    cache = {}

    def execute(mem):
        if mem in cache:
            return cache[mem]

        cmd = get_cmd(mem)
        process = subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        return_code = process.returncode
        if return_code == 42:
            print("it return 42")
            exit(1)
        cache[mem] = return_code
        return return_code

    l = 1
    r = 64
    max = 4*1024
    while r <= max:
        return_code = execute(r)
        if return_code != 0:
            l = r
            r *= 2
        else:
            break
    print(f"calculate r = {r}")
    if r == 2*max:
        return r


    while l < r - 1:
        m = (l + r) // 2
        return_code = execute(m)
        print(f"for {m} mem get code {return_code}")

        if return_code != 0:
            l = m
        else:
            r = m

    return l


files = os.listdir(files_dir)

print(tool)
with open(f"{tool}_res.txt", "w") as output:
    output.write(f"file,mem\n")
    output.flush()
    for file in files:
        mem = run_tool_for_file(tool, files_dir + file)
        print(f"Got for tool = {tool}, file = {file}: {mem}mb")
        output.write(f"{file},{mem}\n")
        output.flush()
