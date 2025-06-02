import subprocess
import itertools
from pathlib import Path

# Konfiguration
infras = ["testgraph50.yml", "testgraph250.yml", "testgraph500.yml", "testgraph100.yml", "testgraph1000.yml", "testgraph5000.yml"]
features = ["knowledge-sharing", "local", "full", "auctioning"]
scenarios = ["large", "risk-introduction", "growing", "unstable", "mixed", "no-change"]



# Vollqualifizierter Klassenname
java_class = "tech.jorn.adrian.experiment.SingleNodeInfra"

# Pfad zur Klasse (kompilierte .class-Dateien)
classpath = "C:\\Users\\engel\\IdeaProjects\\adrian-6565\\experiment\\src\\main\\java\\tech\\jorn\\adrian\\experiment" 

# Alle Kombinationen durchgehen
for yaml, features, scenario in itertools.product(infras, features, scenarios):
    print(f"Starte Test mit: {yaml}, {features}, {scenario}")
    subprocess.run([
        "java", "-cp", classpath, java_class,
        yaml, scenario, features
    ])