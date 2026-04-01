import React, { useState } from 'react';
import ExpenseService from '../services/ExpenseService';

const Login = ({ onLogin }) => {
    const [credentials, setCredentials] = useState({
        username: '',
        password: ''
    });
    const [isRegistering, setIsRegistering] = useState(false);
    const [registerData, setRegisterData] = useState({
        username: '',
        email: '',
        password: ''
    });
    const [message, setMessage] = useState('');
    const [loading, setLoading] = useState(false);

    const handleLoginChange = (e) => {
        setCredentials({
            ...credentials,
            [e.target.name]: e.target.value
        });
    };

    const handleRegisterChange = (e) => {
        setRegisterData({
            ...registerData,
            [e.target.name]: e.target.value
        });
    };

    const handleLogin = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            const response = await ExpenseService.login(credentials);
            localStorage.setItem("user", JSON.stringify(response.data));
            onLogin(response.data);
            setMessage('');
        } catch (error) {
            setMessage(error.response?.data?.message || 'Login failed');
        }
        setLoading(false);
    };

    const handleRegister = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            await ExpenseService.register(registerData);
            setMessage('Registration successful! Please login.');
            setIsRegistering(false);
            setRegisterData({ username: '', email: '', password: '' });
        } catch (error) {
            setMessage(error.response?.data?.message || 'Registration failed');
        }
        setLoading(false);
    };

    return (
        <div className="login-container">
            <div className="login-card">
                <h2 className="login-title">{isRegistering ? 'Create Account' : 'Welcome Back'}</h2>

                {message && (
                    <div className={`message ${message.includes('successful') ? 'success' : 'error'}`}>
                        {message}
                    </div>
                )}

                {!isRegistering ? (
                    <form onSubmit={handleLogin} className="login-form">
                        <div className="form-group">
                            <label>Username</label>
                            <input
                                type="text"
                                name="username"
                                placeholder="Enter your username"
                                value={credentials.username}
                                onChange={handleLoginChange}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label>Password</label>
                            <input
                                type="password"
                                name="password"
                                placeholder="Enter your password"
                                value={credentials.password}
                                onChange={handleLoginChange}
                                required
                            />
                        </div>
                        <button type="submit" disabled={loading} className="btn-primary">
                            {loading ? 'Signing in...' : 'Sign In'}
                        </button>
                    </form>
                ) : (
                    <form onSubmit={handleRegister} className="login-form">
                        <div className="form-group">
                            <label>Username</label>
                            <input
                                type="text"
                                name="username"
                                placeholder="Choose a username"
                                value={registerData.username}
                                onChange={handleRegisterChange}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label>Email</label>
                            <input
                                type="email"
                                name="email"
                                placeholder="Enter your email"
                                value={registerData.email}
                                onChange={handleRegisterChange}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label>Password</label>
                            <input
                                type="password"
                                name="password"
                                placeholder="Create a password"
                                value={registerData.password}
                                onChange={handleRegisterChange}
                                required
                            />
                        </div>
                        <button type="submit" disabled={loading} className="btn-secondary">
                            {loading ? 'Creating account...' : 'Create Account'}
                        </button>
                    </form>
                )}

                <div className="auth-toggle">
                    <button
                        type="button"
                        onClick={() => {
                            setIsRegistering(!isRegistering);
                            setMessage('');
                        }}
                        className="link-button"
                    >
                        {isRegistering ? 'Already have an account? Sign In' : 'Need an account? Sign Up'}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default Login;