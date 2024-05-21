for dataset in /home/olga/gllgen/dataset_black_box/too_little #/home/olga/gllgen/java7
do
  for tool in Antlr Online Offline
  do
    echo "running $tool on $dataset, start at $(date)"
    gradle benchmark -PtoolName=$tool -Pdataset=$dataset >> stdout_$tool.txt 2>> stderr_$tool.txt
  done
done

