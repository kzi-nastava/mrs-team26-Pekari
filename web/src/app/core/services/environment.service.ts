import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

export interface EnvironmentConfig {
  production: boolean;
  apiUrl: string;
  appName: string;
  logLevel: 'debug' | 'info' | 'warn' | 'error';
  enableDevTools: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class EnvironmentService {
  private readonly config: EnvironmentConfig = environment;

  /**
   * Get the current environment configuration
   */
  getConfig(): EnvironmentConfig {
    return this.config;
  }

  /**
   * Get the API URL for backend calls
   */
  getApiUrl(): string {
    return this.config.apiUrl;
  }

  /**
   * Check if the application is running in production mode
   */
  isProduction(): boolean {
    return this.config.production;
  }

  /**
   * Check if development tools are enabled
   */
  areDevToolsEnabled(): boolean {
    return this.config.enableDevTools;
  }

  /**
   * Get the current log level
   */
  getLogLevel(): string {
    return this.config.logLevel;
  }

  /**
   * Get the application name
   */
  getAppName(): string {
    return this.config.appName;
  }
}
