import numpy as np
import matplotlib.pyplot as plt
import pykrige.kriging_tools as kt
from pykrige.ok import OrdinaryKriging

x = np.array([-100, 280, -290, 23, 101, 110])
y = np.array([56, 100, 590, 470, 200, 25])
phi = np.array([29.3, 21.0, 19.2, 29.1, 21.9, 23.1])

cax = plt.scatter(x, y, c=phi)
cbar = plt.colorbar(cax, fraction=0.03)
plt.title('Measured Porosity')

OK = OrdinaryKriging(
    x, 
    y, 
    phi, 
    variogram_model='gaussian',
    verbose=True,
    enable_plotting=True,
    nlags=10,
)

OK.variogram_model_parameters

gridx = np.arange(-300, 300, 10, dtype='float64')
gridy = np.arange(0, 600, 10, dtype='float64')
zstar, ss = OK.execute("grid", gridx, gridy)

print(zstar.shape)
print(ss.shape)

cax = plt.imshow(zstar, extent=(-300, 300, 0, 600), origin='lower')
plt.scatter(x, y, c='k', marker='.')
cbar=plt.colorbar(cax)
plt.title('Porosity estimate')

cax = plt.imshow(np.sqrt(ss), extent=(-300, 300, 0, 600), origin='lower', vmin = 0)
plt.scatter(x, y, c='k', marker='.')
cbar=plt.colorbar(cax)
plt.title('Porosity standard devation')

OK.variogram_model_parameters
