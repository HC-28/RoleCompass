import os, pandas as pd, numpy as np, json

# Paths derived from this script’s location — works on any machine.
_HERE  = os.path.dirname(os.path.abspath(__file__))
BASE   = os.path.join(_HERE, "dataset")
KAGGLE = os.path.join(os.path.dirname(_HERE), "dataset")

RIASEC_RAW = {
    '15-1252.00':{'R':3.61,'I':6.05,'A':2.37,'S':1.81,'E':1.87,'C':5.62},
    '15-1251.00':{'R':3.37,'I':5.10,'A':2.51,'S':1.98,'E':2.09,'C':5.92},
    '15-1254.00':{'R':2.98,'I':5.01,'A':3.13,'S':2.22,'E':2.96,'C':5.03},
    '15-1255.00':{'R':2.59,'I':4.88,'A':4.48,'S':2.20,'E':3.13,'C':4.40},
    '15-2051.00':{'R':2.17,'I':6.98,'A':2.61,'S':1.66,'E':1.71,'C':5.39},
    '15-2051.01':{'R':1.24,'I':5.41,'A':1.93,'S':1.85,'E':4.42,'C':5.79},
    '15-2051.02':{'R':1.25,'I':5.66,'A':1.27,'S':2.82,'E':3.30,'C':6.58},
    '15-1243.00':{'R':2.63,'I':5.63,'A':2.16,'S':1.91,'E':2.68,'C':6.12},
    '15-1242.00':{'R':2.83,'I':4.83,'A':1.20,'S':2.29,'E':2.94,'C':6.61},
    '15-1243.01':{'R':2.93,'I':4.79,'A':1.62,'S':2.03,'E':3.05,'C':6.38},
    '15-1212.00':{'R':3.56,'I':5.40,'A':1.34,'S':2.11,'E':2.85,'C':6.08},
    '15-1299.05':{'R':4.30,'I':5.54,'A':1.33,'S':1.83,'E':2.66,'C':6.06},
    '15-1244.00':{'R':4.19,'I':4.04,'A':1.00,'S':2.22,'E':3.46,'C':6.18},
    '15-1241.00':{'R':4.04,'I':5.28,'A':2.25,'S':2.17,'E':3.20,'C':5.06},
    '15-1253.00':{'R':3.80,'I':5.76,'A':1.69,'S':1.53,'E':1.57,'C':5.67},
}
ROLE_SOC_WEIGHTED = {
    'Backend Developer':           [('15-1252.00',0.50),('15-1251.00',0.30),('15-1254.00',0.20)],
    'Frontend Developer':          [('15-1255.00',0.55),('15-1254.00',0.45)],
    'Full Stack Developer':        [('15-1252.00',0.40),('15-1254.00',0.35),('15-1255.00',0.25)],
    'Data Scientist':              [('15-2051.00',0.65),('15-2051.01',0.20),('15-2051.02',0.15)],
    'Data Engineer':               [('15-1243.00',0.45),('15-1242.00',0.30),('15-1243.01',0.25)],
    'Cybersecurity Engineer':      [('15-1212.00',0.55),('15-1299.05',0.45)],
    'DevOps Engineer':             [('15-1244.00',0.60),('15-1241.00',0.40)],
    'Cloud Engineer':              [('15-1241.00',0.65),('15-1244.00',0.35)],
    'Android Developer':           [('15-1252.00',0.70),('15-1255.00',0.30)],
    'QA / Test Automation Engineer':[('15-1253.00',0.75),('15-1251.00',0.25)],
}
SO_TECH = {
    'Backend Developer':          [0.90,0.70,0.85,0.15,0.20,0.35,0.25,0.25,0.35,0.20,0.10,0.30,0.05,0.20,0.35,0.40,0.35,0.50,0.70,0.30],
    'Frontend Developer':         [0.15,0.25,0.40,0.95,0.85,0.25,0.10,0.10,0.20,0.10,0.10,0.10,0.20,0.05,0.10,0.30,0.25,0.20,0.55,0.35],
    'Full Stack Developer':       [0.75,0.60,0.75,0.75,0.65,0.45,0.30,0.30,0.45,0.15,0.15,0.35,0.20,0.15,0.20,0.40,0.35,0.40,0.65,0.90],
    'Data Scientist':             [0.20,0.65,0.30,0.10,0.10,0.15,0.10,0.10,0.25,0.95,0.95,0.60,0.05,0.10,0.10,0.15,0.15,0.25,0.60,0.15],
    'Data Engineer':              [0.40,0.90,0.50,0.05,0.10,0.55,0.45,0.50,0.65,0.55,0.35,0.95,0.05,0.10,0.15,0.15,0.20,0.60,0.65,0.15],
    'Cybersecurity Engineer':     [0.45,0.45,0.35,0.05,0.05,0.35,0.50,0.40,0.45,0.20,0.15,0.25,0.05,0.90,0.90,0.30,0.30,0.50,0.50,0.20],
    'DevOps Engineer':            [0.45,0.50,0.45,0.10,0.10,0.95,0.90,0.95,0.75,0.15,0.10,0.55,0.05,0.35,0.50,0.25,0.35,0.90,0.65,0.20],
    'Cloud Engineer':             [0.45,0.65,0.55,0.10,0.10,0.75,0.95,0.90,0.95,0.15,0.10,0.60,0.05,0.40,0.55,0.20,0.25,0.80,0.60,0.20],
    'Android Developer':          [0.55,0.50,0.55,0.65,0.65,0.25,0.15,0.15,0.25,0.10,0.20,0.15,0.95,0.15,0.25,0.35,0.30,0.25,0.60,0.20],
    'QA / Test Automation Engineer':[0.35,0.40,0.40,0.25,0.25,0.45,0.20,0.20,0.20,0.25,0.15,0.25,0.10,0.30,0.30,0.95,0.95,0.40,0.55,0.20],
}
PROJECT_SPECIFIC = {
    'Backend Developer':{'BD':0.35,'SA':0.40,'RO':0.35},
    'Frontend Developer':{'BD':0.40,'SA':0.55,'RO':0.25},
    'Full Stack Developer':{'BD':0.80,'SA':0.60,'RO':0.30},
    'Data Scientist':{'BD':0.30,'SA':0.45,'RO':0.20},
    'Data Engineer':{'BD':0.45,'SA':0.55,'RO':0.30},
    'Cybersecurity Engineer':{'BD':0.55,'SA':0.60,'RO':0.85},
    'DevOps Engineer':{'BD':0.70,'SA':0.75,'RO':0.55},
    'Cloud Engineer':{'BD':0.65,'SA':0.70,'RO':0.45},
    'Android Developer':{'BD':0.45,'SA':0.50,'RO':0.25},
    'QA / Test Automation Engineer':{'BD':0.50,'SA':0.25,'RO':0.70},
}
SKILL_TO_FEATURE = {
    'Python':['STATS','MODEL','PIPELINE'],'Java':['SERVER','API'],'SQL':['STORAGE'],
    'NoSQL':['STORAGE'],'JavaScript':['UI','STATE'],'TypeScript':['UI','STATE'],
    'React':['UI','STATE'],'HTML':['UI'],'CSS':['UI'],'Docker':['CONTAINER','BUILD'],
    'Kubernetes':['CONTAINER','INFRA'],'AWS':['CLOUD','INFRA'],'Azure':['CLOUD','INFRA'],
    'GCP':['CLOUD','INFRA'],'Terraform':['INFRA'],'CI/CD':['BUILD'],'Jenkins':['BUILD'],
    'Machine Learning':['MODEL','STATS'],'TensorFlow':['MODEL'],'PyTorch':['MODEL'],
    'Spark':['PIPELINE'],'Kafka':['PIPELINE'],'Spring':['SERVER','API'],'REST':['API'],
    'Networking':['INFRA'],'Linux':['INFRA','BUILD'],'Android':['MOBILE'],'Kotlin':['MOBILE'],
    'Security':['THREAT','HARDENING'],'Penetration':['THREAT'],'Testing':['TESTDES','TESTAUTO'],
    'Selenium':['TESTAUTO'],'Monitoring':['OBSERV'],'Prometheus':['OBSERV'],'Grafana':['OBSERV'],
    'Performance':['PERF'],'Optimization':['PERF'],'Full Stack':['FULLSPEC'],'Microservices':['SERVER','API'],
}
TECH_FEATS = ['SERVER','STORAGE','API','UI','STATE','BUILD','INFRA','CONTAINER','CLOUD','STATS','MODEL','PIPELINE','MOBILE','THREAT','HARDENING','TESTDES','TESTAUTO','OBSERV','PERF','FULLSPEC']
FEATURE_NAMES = ['R','I','A','S','E','C','DI','TP','BD','SA','RO'] + TECH_FEATS
ROLES = list(ROLE_SOC_WEIGHTED.keys())
NOISE = {'Backend Developer':0.060,'Frontend Developer':0.065,'Full Stack Developer':0.085,
         'Data Scientist':0.060,'Data Engineer':0.065,'Cybersecurity Engineer':0.060,
         'DevOps Engineer':0.075,'Cloud Engineer':0.075,'Android Developer':0.060,
         'QA / Test Automation Engineer':0.060}
ADJ = {
    'Backend Developer':['Full Stack Developer'],'Frontend Developer':['Full Stack Developer'],
    'Full Stack Developer':['Backend Developer','Frontend Developer'],
    'Data Scientist':['Data Engineer'],'Data Engineer':['Data Scientist'],
    'Cybersecurity Engineer':['DevOps Engineer'],'DevOps Engineer':['Cloud Engineer','Cybersecurity Engineer'],
    'Cloud Engineer':['DevOps Engineer'],'Android Developer':['Frontend Developer'],
    'QA / Test Automation Engineer':['Backend Developer'],
}
KAGGLE2_MAP = {
    'Backend Developer':['Backend Developer'],'Frontend Developer':['Frontend Developer','Web Developer'],
    'Full Stack Developer':['Full Stack Java Developer','Full Stack Python Developer'],
    'Data Scientist':['Data Scientist','AIML'],'Data Engineer':[],'Cybersecurity Engineer':['Cybersecurity Engineer'],
    'DevOps Engineer':['DevOps Engineer','Kubernetes Operations Engineer'],'Cloud Engineer':[],
    'Android Developer':['Mobile Developer'],'QA / Test Automation Engineer':[],
}
KAGGLE3_MAP = {
    'Backend Developer':'Backend Developer','Frontend Developer':'Frontend Developer',
    'Full Stack Developer':'Full Stack Developer','Data Scientist':'Data Scientist',
    'Data Engineer':'Data Engineer','Cybersecurity Engineer':'Cybersecurity',
    'DevOps Engineer':'DevOps Engineer','Cloud Engineer':'Cloud Engineer',
    'Android Developer':'Android Developer','QA / Test Automation Engineer':'QA Engineer|Test Automation',
}

def norm7(v): return round(v/7.0,4)
def prediger(R,I,A,S,E,C):
    return round(((C+E)-(I+A)+12)/24,4), round(((R+C)-(S+E)+12)/24,4)

COMPUTED_RIASEC = {}
for role, soc_list in ROLE_SOC_WEIGHTED.items():
    R=I=A=S=E=C=0
    for soc,w in soc_list:
        d=RIASEC_RAW[soc]; R+=w*d['R']; I+=w*d['I']; A+=w*d['A']
        S+=w*d['S']; E+=w*d['E']; C+=w*d['C']
    di,tp = prediger(R,I,A,S,E,C)
    COMPUTED_RIASEC[role]={'R':R,'I':I,'A':A,'S':S,'E':E,'C':C,'DI':di,'TP':tp}

df2 = pd.read_csv(os.path.join(KAGGLE,'candidate_job_role_dataset.csv'))
df3 = pd.read_csv(os.path.join(KAGGLE,'job_dataset.csv'))

FINAL_TECH = {}
calib_rows = []
change_rows = []
for role in ROLES:
    so = np.array(SO_TECH[role])
    sub2 = df2[df2['job_role'].isin(KAGGLE2_MAP.get(role,[]))]
    k3p = KAGGLE3_MAP.get(role,'')
    sub3 = df3[df3['Title'].str.contains(k3p,case=False,na=False)] if k3p else df3.head(0)
    n2,n3 = len(sub2),len(sub3)
    k2=np.zeros(20); k3=np.zeros(20)
    if n2>0:
        for _,r in sub2.iterrows():
            for sk,fl in SKILL_TO_FEATURE.items():
                if sk.lower() in str(r['skills']).lower():
                    for f in fl: k2[TECH_FEATS.index(f)]+=1
        k2=np.clip(k2/n2,0,1)
    if n3>0:
        for _,r in sub3.iterrows():
            text=str(r.get('Skills',''))+' '+str(r.get('Keywords',''))
            for sk,fl in SKILL_TO_FEATURE.items():
                if sk.lower() in text.lower():
                    for f in fl: k3[TECH_FEATS.index(f)]+=1
        k3=np.clip(k3/n3,0,1)
    if n2>0 and n3>0: cal=np.clip(0.50*so+0.35*k3+0.15*k2,0,1)
    elif n3>0: cal=np.clip(0.60*so+0.40*k3,0,1)
    else: cal=so.copy()
    FINAL_TECH[role]=cal.tolist()
    blend='0.50*SO+0.35*K3+0.15*K2' if n2>0 and n3>0 else ('0.60*SO+0.40*K3' if n3>0 else 'SO only')
    row={'Target_Role':role,'Kaggle2_profiles':n2,'Kaggle3_JDs':n3,'Blend_Formula':blend}
    for i,f in enumerate(TECH_FEATS):
        row['SO_'+f]=round(so[i],3); row['Calibrated_'+f]=round(cal[i],3); row['Delta_'+f]=round(cal[i]-so[i],3)
    calib_rows.append(row)
    for i,f in enumerate(TECH_FEATS):
        d=cal[i]-so[i]
        if abs(d)>=0.05:
            change_rows.append({'Role':role,'Feature':f,'SO_value':round(so[i],3),'Calibrated_value':round(cal[i],3),'Change':round(d,3),'Direction':'UP' if d>0 else 'DOWN'})

pd.DataFrame(calib_rows).to_csv(os.path.join(BASE,'03_data_derived','tech_weight_calibration_full.csv'),index=False)
print('[OK] 03_data_derived/tech_weight_calibration_full.csv')
pd.DataFrame(change_rows).to_csv(os.path.join(BASE,'03_data_derived','calibration_significant_changes.csv'),index=False)
print(f'[OK] 03_data_derived/calibration_significant_changes.csv  ({len(change_rows)} changes)')

vec_rows=[]
for role in ROLES:
    cr=COMPUTED_RIASEC[role]; ps=PROJECT_SPECIFIC[role]; tw=FINAL_TECH[role]
    row={'Target_Role':role,'R':norm7(cr['R']),'I':norm7(cr['I']),'A':norm7(cr['A']),
         'S':norm7(cr['S']),'E':norm7(cr['E']),'C':norm7(cr['C']),
         'DI':cr['DI'],'TP':cr['TP'],'BD':ps['BD'],'SA':ps['SA'],'RO':ps['RO']}
    for i,f in enumerate(TECH_FEATS): row[f]=round(tw[i],3)
    vec_rows.append(row)
pd.DataFrame(vec_rows).to_csv(os.path.join(BASE,'03_data_derived','role_center_vectors_31_features.csv'),index=False)
print('[OK] 03_data_derived/role_center_vectors_31_features.csv')

gen_rows=[]
for role in ROLES:
    sub2=df2[df2['job_role'].isin(KAGGLE2_MAP.get(role,[]))]
    gen_rows.append({'Target_Role':role,'Noise_Sigma':NOISE[role],'Overlap_Rate_20pct':True,'Adjacent_Roles':str(ADJ.get(role,[])),'Synthetic_Rows':350,'Real_Rows_from_Kaggle':min(len(sub2),80),'Total':350+min(len(sub2),80)})
pd.DataFrame(gen_rows).to_csv(os.path.join(BASE,'03_data_derived','generation_parameters.csv'),index=False)
print('[OK] 03_data_derived/generation_parameters.csv')

np.random.seed(42)
ROLE_VECS={}
for role in ROLES:
    cr=COMPUTED_RIASEC[role]; ps=PROJECT_SPECIFIC[role]; tw=FINAL_TECH[role]
    ROLE_VECS[role]=np.array([norm7(cr['R']),norm7(cr['I']),norm7(cr['A']),norm7(cr['S']),norm7(cr['E']),norm7(cr['C']),cr['DI'],cr['TP'],ps['BD'],ps['SA'],ps['RO']]+tw)

all_rows=[]
stats=[]
for role in ROLES:
    target=ROLE_VECS[role]; sigma=NOISE[role]; synth_n=0; real_n=0; ov_n=0
    for _ in range(350):
        p=target+np.random.normal(0,sigma,31)
        if np.random.rand()<0.20 and ADJ.get(role):
            adj=np.random.choice(ADJ[role]); alpha=np.random.uniform(0.10,0.30)
            p=(1-alpha)*p+alpha*ROLE_VECS[adj]; ov_n+=1
        all_rows.append(list(np.clip(p,0,1))+[role,'synthetic']); synth_n+=1
    sub2=df2[df2['job_role'].isin(KAGGLE2_MAP.get(role,[]))].head(80)
    for _,kr in sub2.iterrows():
        v=target.copy()
        for sk,fl in SKILL_TO_FEATURE.items():
            if sk.lower() in str(kr['skills']).lower():
                for f in fl: idx=FEATURE_NAMES.index(f); v[idx]=min(v[idx]+0.15,1.0)
        all_rows.append(list(np.clip(v+np.random.normal(0,0.04,31),0,1))+[role,'real_kaggle']); real_n+=1
    stats.append({'Role':role,'Synthetic':synth_n,'Real_Kaggle':real_n,'Overlap_blended':ov_n,'Total':synth_n+real_n})

df_final=pd.DataFrame(all_rows,columns=FEATURE_NAMES+['role','source'])
g=df_final.groupby('role')
checks=[
    ('Backend highest SERVER',g['SERVER'].mean().idxmax()=='Backend Developer'),
    ('Frontend highest UI',g['UI'].mean().idxmax()=='Frontend Developer'),
    ('DataSci highest MODEL',g['MODEL'].mean().idxmax()=='Data Scientist'),
    ('DataEng highest PIPELINE',g['PIPELINE'].mean().idxmax()=='Data Engineer'),
    ('Cyber highest THREAT',g['THREAT'].mean().idxmax()=='Cybersecurity Engineer'),
    ('QA highest TESTAUTO',g['TESTAUTO'].mean().idxmax()=='QA / Test Automation Engineer'),
    ('Android highest MOBILE',g['MOBILE'].mean().idxmax()=='Android Developer'),
    ('DevOps highest BUILD',g['BUILD'].mean().idxmax()=='DevOps Engineer'),
    ('Cloud highest CLOUD',g['CLOUD'].mean().idxmax()=='Cloud Engineer'),
    ('All in range',bool((df_final[FEATURE_NAMES]>=0).all().all() and (df_final[FEATURE_NAMES]<=1).all().all())),
]
passed=sum(1 for _,ok in checks if ok)
for desc,ok in checks:
    status='[PASS]' if ok else '[FAIL]'
    print(f'{status}  {desc}')
print(f'Sanity: {passed}/{len(checks)} passed')

df_final.to_csv(os.path.join(BASE,'04_final_dataset','rolecompass_full_with_source_column.csv'),index=False)
df_final[df_final['source']=='synthetic'].drop('source',axis=1).to_csv(os.path.join(BASE,'04_final_dataset','rolecompass_synthetic_only.csv'),index=False)
df_final[df_final['source']=='real_kaggle'].drop('source',axis=1).to_csv(os.path.join(BASE,'04_final_dataset','rolecompass_real_kaggle_only.csv'),index=False)
pd.DataFrame(stats).to_csv(os.path.join(BASE,'04_final_dataset','per_role_composition.csv'),index=False)
df_final.drop('source',axis=1).to_csv(os.path.join(BASE,'rolecompass_synthetic.csv'),index=False)

synth_count=int((df_final['source']=='synthetic').sum())
real_count=int((df_final['source']=='real_kaggle').sum())
print(f'[OK] rolecompass_full_with_source_column.csv  ({len(df_final)} rows)')
print(f'[OK] rolecompass_synthetic_only.csv  ({synth_count} rows)')
print(f'[OK] rolecompass_real_kaggle_only.csv  ({real_count} rows)')
print(f'[OK] per_role_composition.csv')
print(f'[OK] rolecompass_synthetic.csv updated  ({len(df_final)} rows total)')

schema={'version':'3.0','total_rows':len(df_final),'synthetic_rows':synth_count,'real_kaggle_rows':real_count,
        'features':31,'classes':10,
        'soc_codes_used':list(RIASEC_RAW.keys()),'feature_names':FEATURE_NAMES,'roles':ROLES}
with open(os.path.join(BASE,'feature_schema.json'),'w') as f: json.dump(schema,f,indent=2)
print('[OK] feature_schema.json v3.0')
print('ALL DONE')
