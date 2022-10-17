import numpy as np
from sklearn.gaussian_process import GaussianProcessRegressor
from sklearn.gaussian_process.kernels import RBF
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
import pandas as pd

data = pd.read_csv("datosDelitos.csv")

n_samples = 2000

data = data.sample(n_samples, random_state=2)

x = np.asanyarray(data.drop(columns=['delito','porcentaje']))[:n_samples,:]
y = np.asanyarray(data[['porcentaje']])[:n_samples].ravel()

xtrain, xtest, ytrain, ytest = train_test_split(x,y)

kernel = 2.0 * RBF(1.0)

model = Pipeline([('scaler',StandardScaler()),
                  ('kriging',GaussianProcessRegressor(kernel=kernel,alpha=0.01))])

model.fit(xtrain, ytrain)

print('Train: ', model.score(xtrain, ytrain))
print('Test: ', model.score(xtest,ytest))

xnew = np.array([[-103.4158524, 20.69687707]]).reshape(1,-1)
ynew = model.predict(xnew)

print(ynew)

# import pickle 
# pickle.dump(model, open('modeloPorcentajeDelitos.sav', 'wb'))
